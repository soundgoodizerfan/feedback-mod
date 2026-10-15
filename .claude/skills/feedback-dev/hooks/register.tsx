import { atom, read, update } from 'claude-code'
import type { EngineInterface, Register } from 'claude-code'

import type { Build, Check, DocsRead } from '../types'

// Feedback's dev loop, in place of three settings shell hooks:
// - the design-doc gate: no edit under src/main/ until this loop (main or a
//   subagent, each on its own) has Read feedback_philosophy.md and
//   feedback_mechanics.md -- CLAUDE.md calls them required reading;
// - one compileJava per turn instead of per edit, at Stop/SubagentStop, its
//   errors handed back as a block so the model fixes them before it stops;
// - compile / game test / deploy state on the status line;
// - a band with the gate's state and an /open-work pane (TODO.md items and
//   the [OPEN] count per design document).

const PLUGIN = 'feedback-dev'
const PANE = 'open-work'
const EDITS = new Set(['Edit', 'Write', 'MultiEdit'])
const DOCS = { philosophy: 'feedback_philosophy.md', mechanics: 'feedback_mechanics.md' } as const

const docs = atom({ plugin: 'feedback-dev', key: 'docs' } as const, {})
const dirty = atom({ plugin: 'feedback-dev', key: 'dirty' } as const, {})
const build = atom({ plugin: 'feedback-dev', key: 'build' } as const, {})
const todo = atom({ plugin: 'feedback-dev', key: 'todo' } as const, '')

type $ = EngineInterface

const loop = (agent: string | undefined) => agent ?? 'main'

const filePath = (input: unknown) => {
  const path = (input as { file_path?: unknown } | null)?.file_path
  return typeof path === 'string' ? path : ''
}

const ago = (now: number, at: number) => {
  const minutes = Math.round((now - at) / 60000)
  return minutes < 60 ? `${minutes}m` : `${Math.round(minutes / 60)}h`
}

// Same filter compile-check.sh used: the error lines and two after each.
const errors = (out: string) => {
  const lines = out.split('\n')
  const kept: string[] = []
  lines.forEach((line, i) => {
    if (/error:|warning: \[|FAILED|What went wrong/.test(line)) kept.push(...lines.slice(i, i + 3))
  })
  return kept.slice(0, 40).join('\n')
}

// The repo root, or '' outside the Feedback repo (every hook then passes).
let root = ''


const showStatus = async ($: $) => {
  const now = await $.clock.now()
  const b: Build = await read($, build)
  const check = (name: string, c: Check | undefined) =>
    c ? `${name} ${c.ok ? 'ok' : 'FAIL'} ${ago(now, c.at)}` : `${name} -`
  const parts = [
    b.compile === 'running' ? 'compile ...' : check('compile', b.compile),
    check('tests', b.tests),
    `deploy ${b.deploy ?? '-'}`,
  ]
  $.ui.status(parts.join(' · '))
}

const setBuild = async ($: $, patch: Partial<Build>) => {
  await update($, build, b => ({ ...b, ...patch }))
  await showStatus($)
}

// Stale = any source or build file newer than the jar in the test pack.
const refreshDeploy = async ($: $) => {
  const mods = `${root}/../feedback-test-pack/mods`
  const jar = (await $.fs.exists(mods))
    ? (await $.fs.list(mods)).find(f => /^feedback-.*\.jar$/.test(f.name))
    : undefined
  if (!jar) return setBuild($, { deploy: 'none' })
  const { stdout } = await $.process.run(
    ['find', 'src', 'build.gradle', 'gradle.properties', '-newer', `${mods}/${jar.name}`, '-type', 'f', '-print', '-quit'],
    { cwd: root },
  )
  await setBuild($, { deploy: stdout.trim() ? 'stale' : 'fresh' })
}

const refreshTodo = async ($: $) => {
  const counts: string[] = []
  for (const f of await $.fs.list(root)) {
    if (!f.name.endsWith('.md') || f.name === 'TODO.md' || f.name === 'CLAUDE.md') continue
    const n = (await $.fs.read(`${root}/${f.name}`)).split('[OPEN]').length - 1
    if (n > 0) counts.push(`${f.name.replace(/^feedback_|\.md$/g, '')} ${n}`)
  }
  // Headings and open items with their indented continuation lines.
  const items: string[] = []
  let inItem = false
  for (const line of (await $.fs.read(`${root}/TODO.md`)).split('\n')) {
    if (/^#{2,}\s/.test(line)) { items.push('', line); inItem = false }
    else if (/^\s*- \[ \]/.test(line)) { items.push(line); inItem = true }
    else if (inItem && /^\s+\S/.test(line)) items.push(line)
    else inItem = false
  }
  await update($, todo, () => `**[OPEN] in docs:** ${counts.join(' · ')}\n${items.join('\n')}`)
}

const compile = async ($: $, agent: string) => {
  const roots = (await read($, dirty))[agent] ?? []
  if (roots.length === 0) return undefined
  await update($, dirty, d => ({ ...d, [agent]: [] }))
  await setBuild($, { compile: 'running' })
  const failures: string[] = []
  for (const r of roots) {
    const run = await $.process.run(['./gradlew', 'compileJava', '-q', '--console=plain'], {
      cwd: r,
      timeoutMs: 300000,
    })
    if (run.exitCode !== 0) failures.push(`compileJava failed in ${r}:\n${errors(run.stdout + run.stderr)}`)
  }
  await setBuild($, { compile: { ok: failures.length === 0, at: await $.clock.now() } })
  await refreshDeploy($)
  return failures.length ? failures.join('\n\n') : undefined
}

const stop = async ($: $, agent: string) => {
  const failed = await compile($, agent)
  return failed ? { block: failed } : undefined
}

export const register: Register = on => {
  on('session.start', async ($, e, next) => {
    root = await $.session.cwd()
    if (!(await $.fs.exists(`${root}/${DOCS.philosophy}`))) {
      root = ''
      return next(e)
    }
    await $.command.register({ name: PANE, description: 'Open work: TODO.md items and [OPEN] counts' })
    await refreshTodo($)
    await refreshDeploy($)
    return next(e)
  })

  on('command.run', { command: PANE }, async $ => {
    if (!root) return { text: 'Not in the Feedback repo.' }
    await refreshTodo($)
    await $.ui.open({ id: PANE, title: 'Open work' })
    return { text: 'Open work pane opened.' }
  })

  // Record design-doc reads and edited source roots, per loop.
  on('classic.PostToolUse', async ($, e, next) => {
    if (root) {
      const agent = loop(e.agent_id)
      const path = filePath(e.tool_input)
      if (e.tool_name === 'Read') {
        for (const [doc, file] of Object.entries(DOCS)) {
          if (path.endsWith(`/${file}`)) {
            await update($, docs, d => ({
              ...d,
              [agent]: { philosophy: false, mechanics: false, ...d[agent], [doc]: true },
            }))
          }
        }
      } else if (EDITS.has(e.tool_name) && path.endsWith('.java') && path.includes('/src/')) {
        const r = path.slice(0, path.indexOf('/src/'))
        await update($, dirty, d => ({ ...d, [agent]: [...new Set([...(d[agent] ?? []), r])] }))
      }
    }
    return next(e)
  })

  on('tool.check', async ($, e, next) => {
    const path = filePath(e.input)
    if (!root || !EDITS.has(e.tool) || !path.includes('/src/main/')) return next(e)
    const read_: DocsRead | undefined = (await read($, docs))[loop(e.agentId)]
    const missing = Object.entries(DOCS)
      .filter(([doc]) => !read_?.[doc as keyof DocsRead])
      .map(([, file]) => file)
    if (missing.length === 0) return next(e)
    return {
      decision: 'deny',
      reason: `Blocked: this session hasn't Read ${missing.join(', ')} yet. CLAUDE.md: these are authoritative and required before any implementation in this repo. Read them (Read tool, not a summary), then retry the edit.`,
    }
  }).catch(() => ({ decision: 'deny', reason: `${PLUGIN}: the design-doc gate failed.` }))

  on('classic.Stop', async ($, e, next) => {
    const result = await next(e)
    const blocked = root ? await stop($, 'main') : undefined
    return blocked ? { ...result, ...blocked } : result
  })

  on('classic.SubagentStop', async ($, e, next) => {
    const result = await next(e)
    const blocked = root ? await stop($, loop(e.agent_id)) : undefined
    return blocked ? { ...result, ...blocked } : result
  })

  // Builds, game tests and deploys run by hand through Bash.
  on('tool.call', { tool: 'Bash' }, async ($, e, next) => {
    const ran = await next(e)
    if (!root || ran.deny !== undefined) return ran
    const ok = ran.isError !== true
    const at = await $.clock.now()
    if (/runGameTestServer/.test(String(e.command))) await setBuild($, { tests: { ok, at } })
    else if (/gradlew\b.*\b(build|compileJava)\b/.test(String(e.command))) await setBuild($, { compile: { ok, at } })
    if (/build-and-deploy\.sh|gradlew/.test(String(e.command))) await refreshDeploy($)
    return ran
  })

  on('turn.complete', async ($, e, next) => {
    if (root && e.agentId === undefined) {
      await refreshTodo($)
      await refreshDeploy($)
    }
    return next(e)
  })

  on('ui.render', { component: 'AbovePrompt' }, async ($, e, next) => {
    if (!root || e.props.hasSurvey) return next(e)
    const { Box, Text } = $.ui.resolve(e)
    const main = (await read($, docs)).main
    const mark = (ok: boolean | undefined) => (ok ? 'read' : 'NOT READ')
    if (main?.philosophy && main.mechanics) {
      return <Box><Text dimColor>design docs read</Text></Box>
    }
    return (
      <Box>
        <Text color="yellow">
          design docs: philosophy {mark(main?.philosophy)} · mechanics {mark(main?.mechanics)} -- src/main edits blocked
        </Text>
      </Box>
    )
  })

  on('ui.render', { component: 'Pane', requestId: PANE }, async ($, e) => {
    const { Markdown } = $.ui.resolve(e)
    return <Markdown text={(await read($, todo)) || 'Loading...'} />
  })
}
