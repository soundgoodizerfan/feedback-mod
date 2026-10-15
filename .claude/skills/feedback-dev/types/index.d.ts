export type Check = { ok: boolean; at: number }
export type DocsRead = { philosophy: boolean; mechanics: boolean }
export type Build = {
  compile?: Check | 'running'
  tests?: Check
  deploy?: 'fresh' | 'stale' | 'none'
}

declare module 'claude-code' {
  interface PluginState {
    'feedback-dev': {
      docs: Record<string, DocsRead>
      dirty: Record<string, string[]>
      build: Build
      todo: string
    }
  }
}
