import { expect, test } from 'claude-code/testing'
import type { On } from 'claude-code'
import type { Engine } from 'claude-code/testing'

const ROOT = '/repo'
const EDIT = { tool: 'Edit', input: { file_path: `${ROOT}/src/main/A.java` } }

// The engine beneath: a Feedback checkout whose compile fails when told to.
const engine = (on: On, compileOk = true) => {
  on('session.start', ($, e) => ({ cwd: e.cwd }))
  on('session.cwd', () => ({ value: ROOT }))
  on('clock.now', () => ({ value: 0 }))
  on('classic.PostToolUse', () => ({}))
  on('classic.Stop', () => ({}))
  on('fs.exists', () => ({ value: true }))
  on('fs.list', () => ({ value: [] }))
  on('fs.read', () => ({ value: '' }))
  on('command.register', ($, e) => ({ value: { command: e.name } }))
  on('tool.check', () => ({ decision: 'allow' as const }))
  on('process.run', ($, e) => ({
    value: {
      exitCode: e.argv[0] === './gradlew' && !compileOk ? 1 : 0,
      stdout: e.argv[0] === './gradlew' ? 'A.java:3: error: ; expected' : '',
      stderr: '',
      isStdoutTruncated: false,
      isStderrTruncated: false,
    },
  }))
}

const start = ($: Engine) =>
  $.session.start({ cwd: ROOT, surface: null, isInteractive: true })

const post = (tool_name: string, file_path: string, agent_id?: string) => ({
  tool_name,
  tool_input: { file_path },
  tool_response: {},
  tool_use_id: 't',
  ...(agent_id ? { agent_id } : {}),
})

test('src/main edits wait for both design docs, per loop', async ($, on) => {
  engine(on)
  await start($)
  expect((await $.tool.check(EDIT)).decision).toBe('deny')
  await $.classic.PostToolUse(post('Read', `${ROOT}/feedback_philosophy.md`))
  expect((await $.tool.check(EDIT)).decision).toBe('deny')
  await $.classic.PostToolUse(post('Read', `${ROOT}/feedback_mechanics.md`))
  expect((await $.tool.check(EDIT)).decision).toBe('allow')
  expect((await $.tool.check({ ...EDIT, agentId: 'sub' })).decision).toBe('deny')
  expect((await $.tool.check({ tool: 'Edit', input: { file_path: `${ROOT}/TODO.md` } })).decision).toBe('allow')
})

test('a failing compile blocks the stop once', async ($, on) => {
  engine(on, false)
  await start($)
  await $.classic.PostToolUse(post('Edit', `${ROOT}/src/main/A.java`))
  const first = await $.classic.Stop({ stop_hook_active: false })
  expect(first.block).toMatch(/error: ; expected/)
  const second = await $.classic.Stop({ stop_hook_active: true })
  expect(second.block).toBe(undefined)
})
