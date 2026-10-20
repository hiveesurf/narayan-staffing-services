import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { lazy, Suspense } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { Toaster } from 'sonner'
import { AdminLayout } from './components/AdminLayout'
import { PublicLayout } from './components/PublicLayout'
import { AuthProvider, useAuth } from './lib/auth'
import { ApplicationDetailPage, ApplicationsPage, DashboardPage, ForgotPage, JobApplicantsPage, JobFormPage, JobsPage, LoginPage, ResetPage } from './pages/admin/Core'
import { CandidateDetailPage, CandidatesPage, CompaniesPage, CompanyDetailPage, EmployeeDetailPage, EmployeesPage, PayrollDetailPage, PayrollPage, PlacementsPage, SalarySlipPage } from './pages/admin/Ops'
import { AboutPage, CareersPage, CategoriesPage, ClientsPage, ContactPage, GalleryPage, HomePage, JobDetailPage, LegalPage, ServiceDetailPage, ServicesPage } from './pages/public/Pages'
import { ApplyPage } from './pages/public/ApplyPage'

const InterviewsPage = lazy(() => import('./pages/admin/Site').then((m) => ({ default: m.InterviewsPage })))
const SalaryComponentsPage = lazy(() => import('./pages/admin/Site').then((m) => ({ default: m.SalaryComponentsPage })))
const ServicesAdminPage = lazy(() => import('./pages/admin/Site').then((m) => ({ default: m.ServicesAdminPage })))
const CategoriesAdminPage = lazy(() => import('./pages/admin/Site').then((m) => ({ default: m.CategoriesAdminPage })))
const ClientsAdminPage = lazy(() => import('./pages/admin/Site').then((m) => ({ default: m.ClientsAdminPage })))
const GalleryAdminPage = lazy(() => import('./pages/admin/Site').then((m) => ({ default: m.GalleryAdminPage })))
const TestimonialsAdminPage = lazy(() => import('./pages/admin/Site').then((m) => ({ default: m.TestimonialsAdminPage })))
const IndustriesAdminPage = lazy(() => import('./pages/admin/Site').then((m) => ({ default: m.IndustriesAdminPage })))
const PagesAdminPage = lazy(() => import('./pages/admin/Site').then((m) => ({ default: m.PagesAdminPage })))
const MessagesPage = lazy(() => import('./pages/admin/Site').then((m) => ({ default: m.MessagesPage })))
const UsersPage = lazy(() => import('./pages/admin/Site').then((m) => ({ default: m.UsersPage })))
const AuditPage = lazy(() => import('./pages/admin/Site').then((m) => ({ default: m.AuditPage })))
const ReportsPage = lazy(() => import('./pages/admin/Site').then((m) => ({ default: m.ReportsPage })))
const SettingsPage = lazy(() => import('./pages/admin/Site').then((m) => ({ default: m.SettingsPage })))

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: 1, refetchOnWindowFocus: false } },
})

function Guard() {
  const { user, loading } = useAuth()
  if (loading) return <div className="min-h-dvh bg-paper"><div className="skeleton m-6 h-40 rounded-lg" /></div>
  if (!user) return <Navigate to="/admin/login" replace />
  return <AdminLayout />
}

function Fallback() {
  return <div className="skeleton h-40 rounded-lg" />
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <Suspense fallback={<Fallback />}>
            <Routes>
              <Route element={<PublicLayout />}>
                <Route index element={<HomePage />} />
                <Route path="about" element={<AboutPage />} />
                <Route path="services" element={<ServicesPage />} />
                <Route path="services/:slug" element={<ServiceDetailPage />} />
                <Route path="categories" element={<CategoriesPage />} />
                <Route path="careers" element={<CareersPage />} />
                <Route path="careers/:slug" element={<JobDetailPage />} />
                <Route path="careers/:slug/apply" element={<ApplyPage />} />
                <Route path="gallery" element={<GalleryPage />} />
                <Route path="clients" element={<ClientsPage />} />
                <Route path="contact" element={<ContactPage />} />
                <Route path="privacy-policy" element={<LegalPage page="PRIVACY" />} />
                <Route path="terms" element={<LegalPage page="TERMS" />} />
              </Route>
              <Route path="admin/login" element={<LoginPage />} />
              <Route path="admin/forgot-password" element={<ForgotPage />} />
              <Route path="admin/reset-password" element={<ResetPage />} />
              <Route path="admin" element={<Guard />}>
                <Route index element={<Navigate to="dashboard" replace />} />
                <Route path="dashboard" element={<DashboardPage />} />
                <Route path="jobs" element={<JobsPage />} />
                <Route path="jobs/new" element={<JobFormPage />} />
                <Route path="jobs/:id/edit" element={<JobFormPage />} />
                <Route path="jobs/:id" element={<JobApplicantsPage />} />
                <Route path="applications" element={<ApplicationsPage />} />
                <Route path="applications/:id" element={<ApplicationDetailPage />} />
                <Route path="candidates" element={<CandidatesPage />} />
                <Route path="candidates/:id" element={<CandidateDetailPage />} />
                <Route path="interviews" element={<InterviewsPage />} />
                <Route path="companies" element={<CompaniesPage />} />
                <Route path="companies/:id" element={<CompanyDetailPage />} />
                <Route path="placements" element={<PlacementsPage />} />
                <Route path="employees" element={<EmployeesPage />} />
                <Route path="employees/:id" element={<EmployeeDetailPage />} />
                <Route path="payroll" element={<PayrollPage />} />
                <Route path="payroll/slips/:recordId" element={<SalarySlipPage />} />
                <Route path="payroll/:id" element={<PayrollDetailPage />} />
                <Route path="salary-slips" element={<Navigate to="/admin/payroll" replace />} />
                <Route path="salary-components" element={<SalaryComponentsPage />} />
                <Route path="services" element={<ServicesAdminPage />} />
                <Route path="categories" element={<CategoriesAdminPage />} />
                <Route path="clients" element={<ClientsAdminPage />} />
                <Route path="gallery" element={<GalleryAdminPage />} />
                <Route path="testimonials" element={<TestimonialsAdminPage />} />
                <Route path="industries" element={<IndustriesAdminPage />} />
                <Route path="pages" element={<PagesAdminPage />} />
                <Route path="contact-messages" element={<MessagesPage />} />
                <Route path="users" element={<UsersPage />} />
                <Route path="audit-logs" element={<AuditPage />} />
                <Route path="reports" element={<ReportsPage />} />
                <Route path="settings" element={<SettingsPage />} />
              </Route>
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </Suspense>
        </BrowserRouter>
        <Toaster position="top-right" />
      </AuthProvider>
    </QueryClientProvider>
  )
}
