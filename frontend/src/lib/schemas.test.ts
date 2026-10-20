import { describe, expect, it } from 'vitest'
import { applySchema, loginSchema } from './schemas'

describe('forms', () => {
  it('requires consent on a job application', () => {
    const result = applySchema.safeParse({ fullName: 'Asha Patil', mobile: '9876543210', email: 'asha@example.com', consent: false })
    expect(result.success).toBe(false)
  })

  it('accepts a complete minimum application', () => {
    const result = applySchema.safeParse({ fullName: 'Asha Patil', mobile: '9876543210', email: 'asha@example.com', consent: true })
    expect(result.success).toBe(true)
  })

  it('rejects a login without email', () => {
    expect(loginSchema.safeParse({ email: 'not-an-email', password: 'x' }).success).toBe(false)
  })
})
