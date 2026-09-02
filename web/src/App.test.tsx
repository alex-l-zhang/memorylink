import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import App from './App'

describe('App（B 端占位页）', () => {
  it('渲染机构后台标题与占位说明', () => {
    render(<App />)
    expect(screen.getByRole('heading', { name: '忆联 · 机构后台（B 端）' })).toBeInTheDocument()
    expect(screen.getByText(/工程脚手架已就绪/)).toBeInTheDocument()
  })
})
