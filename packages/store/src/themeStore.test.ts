import { beforeEach, describe, expect, it } from 'vitest'
import { useThemeStore } from './themeStore'

describe('themeStore', () => {
  beforeEach(() => {
    useThemeStore.getState().setTheme('light')
  })

  it('defaults to light', () => {
    expect(useThemeStore.getState().theme).toBe('light')
  })

  it('setTheme sets the theme directly', () => {
    useThemeStore.getState().setTheme('dark')

    expect(useThemeStore.getState().theme).toBe('dark')
  })

  it('toggleTheme flips light to dark', () => {
    useThemeStore.getState().toggleTheme()

    expect(useThemeStore.getState().theme).toBe('dark')
  })

  it('toggleTheme flips dark back to light', () => {
    useThemeStore.getState().setTheme('dark')

    useThemeStore.getState().toggleTheme()

    expect(useThemeStore.getState().theme).toBe('light')
  })
})
