import { z } from 'zod'

export const loginSchema = z.object({
  email: z.string().email('Enter a valid email'),
  password: z.string().min(1, 'Password is required'),
})

export const applySchema = z.object({
  fullName: z.string().min(2, 'Enter your full name'),
  dateOfBirth: z.string().optional(),
  gender: z.string().optional(),
  mobile: z.string().min(10, 'Enter a valid mobile number'),
  email: z.string().email('Enter a valid email'),
  alternatePhone: z.string().optional(),
  address: z.string().optional(),
  city: z.string().optional(),
  state: z.string().optional(),
  pincode: z.string().optional(),
  highestQualification: z.string().optional(),
  specialization: z.string().optional(),
  totalExperience: z.string().optional(),
  relevantExperience: z.string().optional(),
  currentCompany: z.string().optional(),
  currentJobTitle: z.string().optional(),
  currentSalary: z.string().optional(),
  expectedSalary: z.string().optional(),
  noticePeriod: z.string().optional(),
  skills: z.string().optional(),
  consent: z.boolean().refine((value) => value, 'Please confirm the information is accurate.'),
})

export const contactSchema = z.object({
  name: z.string().min(2, 'Name is required'),
  email: z.string().email('Enter a valid email'),
  phone: z.string().optional(),
  company: z.string().optional(),
  subject: z.string().min(3, 'Subject is required'),
  message: z.string().min(10, 'Please write a short message'),
})
