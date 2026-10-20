import { zodResolver } from '@hookform/resolvers/zod'
import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useParams } from 'react-router-dom'
import { Button, Field, PageBand, TextInput } from '../../components/desk-ui'
import { api, errorMessage } from '../../lib/api'
import { applySchema } from '../../lib/schemas'

export function ApplyPage() {
  const { slug } = useParams()
  const [step, setStep] = useState(0)
  const [result, setResult] = useState('')
  const [error, setError] = useState('')
  const [resume, setResume] = useState<File | null>(null)
  const [photo, setPhoto] = useState<File | null>(null)
  const [idProof, setIdProof] = useState<File | null>(null)
  const requirements = useQuery({ queryKey: ['doc-req'], queryFn: async () => (await api.get('/api/public/document-requirements')).data })
  const form = useForm({
    resolver: zodResolver(applySchema),
    defaultValues: { fullName: '', mobile: '', email: '', consent: false as boolean },
  })

  async function submit(values: Record<string, unknown>) {
    setError('')
    const data = {
      ...values,
      consent: true,
      totalExperience: values.totalExperience ? Number(values.totalExperience) : null,
      relevantExperience: values.relevantExperience ? Number(values.relevantExperience) : null,
      currentSalary: values.currentSalary ? Number(values.currentSalary) : null,
      expectedSalary: values.expectedSalary ? Number(values.expectedSalary) : null,
      dateOfBirth: values.dateOfBirth || null,
    }
    const body = new FormData()
    body.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }))
    if (resume) body.append('resume', resume)
    if (photo) body.append('photograph', photo)
    if (idProof) body.append('idProof', idProof)
    try {
      const response = await api.post(`/api/public/jobs/${slug}/apply`, body)
      setResult(response.data.applicationNumber)
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  if (typeof document !== 'undefined') {
    document.title = result ? 'Application received | Narayan Staffing Services' : 'Apply | Narayan Staffing Services'
  }

  if (result) {
    return (
      <>
        <PageBand kicker="Application received" title="We have your application." lede="Keep this reference for the desk." />
        <main className="container-page max-w-xl py-12">
          <p className="text-ink-soft">Reference number</p>
          <p className="nums font-display text-3xl">{result}</p>
          <Link to="/careers" className="mt-8 inline-flex font-semibold text-copper">Back to jobs</Link>
        </main>
      </>
    )
  }

  const steps = ['Personal', 'Professional', 'Documents', 'Confirm']
  return (
    <>
    <PageBand kicker="Careers" title="Apply" lede={steps[step]} />
    <main className="container-page max-w-2xl py-12">
      <ol className="flex flex-wrap gap-3 text-sm">
        {steps.map((label, index) => <li key={label} className={index === step ? 'font-semibold text-copper' : 'text-ink-soft'}>{index + 1}. {label}</li>)}
      </ol>
      <form className="mt-8 grid gap-4" onSubmit={form.handleSubmit(submit)}>
        {step === 0 && <>
          <Field label="Full name" required error={form.formState.errors.fullName?.message}><TextInput {...form.register('fullName')} /></Field>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Date of birth"><TextInput type="date" {...form.register('dateOfBirth')} /></Field>
            <Field label="Gender"><TextInput {...form.register('gender')} /></Field>
            <Field label="Mobile" required error={form.formState.errors.mobile?.message}><TextInput {...form.register('mobile')} /></Field>
            <Field label="Email" required error={form.formState.errors.email?.message}><TextInput {...form.register('email')} /></Field>
            <Field label="Alternate phone"><TextInput {...form.register('alternatePhone')} /></Field>
            <Field label="City"><TextInput {...form.register('city')} /></Field>
            <Field label="State"><TextInput {...form.register('state')} /></Field>
            <Field label="Pincode"><TextInput {...form.register('pincode')} /></Field>
          </div>
          <Field label="Address"><TextInput {...form.register('address')} /></Field>
        </>}
        {step === 1 && <>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Highest qualification"><TextInput {...form.register('highestQualification')} /></Field>
            <Field label="Specialization"><TextInput {...form.register('specialization')} /></Field>
            <Field label="Total experience (years)"><TextInput {...form.register('totalExperience')} /></Field>
            <Field label="Relevant experience"><TextInput {...form.register('relevantExperience')} /></Field>
            <Field label="Current company"><TextInput {...form.register('currentCompany')} /></Field>
            <Field label="Current job title"><TextInput {...form.register('currentJobTitle')} /></Field>
            <Field label="Current salary"><TextInput {...form.register('currentSalary')} /></Field>
            <Field label="Expected salary"><TextInput {...form.register('expectedSalary')} /></Field>
          </div>
          <Field label="Notice period"><TextInput {...form.register('noticePeriod')} /></Field>
          <Field label="Skills"><TextInput placeholder="Comma separated" {...form.register('skills')} /></Field>
        </>}
        {step === 2 && (
          <div className="grid gap-4">
            {(requirements.data ?? []).map((item: { code: string; label: string; required: boolean; description?: string }) => (
              <Field key={item.code} label={item.label} required={item.required}>
                <input className="block w-full text-sm" type="file" accept=".pdf,.jpg,.jpeg,.png,.doc,.docx" onChange={(event) => {
                  const file = event.target.files?.[0] ?? null
                  if (item.code === 'RESUME') setResume(file)
                  if (item.code === 'PHOTOGRAPH') setPhoto(file)
                  if (item.code === 'ID_PROOF') setIdProof(file)
                }} />
                {item.description && <span className="text-xs text-ink-soft">{item.description}</span>}
              </Field>
            ))}
          </div>
        )}
        {step === 3 && (
          <label className="flex items-start gap-3 border border-ink bg-card p-4 text-sm">
            <input type="checkbox" className="mt-1" {...form.register('consent')} />
            <span>I confirm that the information provided is accurate.</span>
          </label>
        )}
        {form.formState.errors.consent && <p className="text-sm text-danger">{form.formState.errors.consent.message}</p>}
        {error && <p className="text-sm text-danger">{error}</p>}
        <div className="flex gap-3">
          {step > 0 && <Button type="button" variant="ghost" onClick={() => setStep((value) => value - 1)}>Back</Button>}
          {step < 3 && <Button type="button" onClick={() => setStep((value) => value + 1)}>Continue</Button>}
          {step === 3 && <Button type="submit" disabled={form.formState.isSubmitting}>{form.formState.isSubmitting ? 'Submitting…' : 'Submit application'}</Button>}
        </div>
      </form>
    </main>
    </>
  )
}
