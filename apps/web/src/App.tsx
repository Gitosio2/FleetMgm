import { Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from '@/components/auth/ProtectedRoute'
import { Layout } from '@/components/layout/Layout'
import { Login } from '@/pages/Login'
import { Dashboard } from '@/pages/Dashboard'
import { Clients } from '@/pages/Clients'
import { Vehicles } from '@/pages/Vehicles'
import { Workers } from '@/pages/Workers'
import { Jobs } from '@/pages/Jobs'
import { Workshop } from '@/pages/Workshop'
import { Billing } from '@/pages/Billing'
import { Suppliers } from '@/pages/Suppliers'
import { SupplierInvoices } from '@/pages/SupplierInvoices'
import { AuditLog } from '@/pages/AuditLog'
import { NotImplemented } from '@/pages/NotImplemented'
import { NAV_ITEMS, MANAGEMENT_ROLES } from '@/components/layout/nav-items'

function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />

      <Route
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Dashboard />} />
        <Route
          path="/clients"
          element={
            <ProtectedRoute allowedRoles={MANAGEMENT_ROLES}>
              <Clients />
            </ProtectedRoute>
          }
        />
        <Route
          path="/vehicles"
          element={
            <ProtectedRoute>
              <Vehicles />
            </ProtectedRoute>
          }
        />
        <Route
          path="/workers"
          element={
            <ProtectedRoute>
              <Workers />
            </ProtectedRoute>
          }
        />
        <Route
          path="/jobs"
          element={
            <ProtectedRoute allowedRoles={[...MANAGEMENT_ROLES, 'DRIVER']}>
              <Jobs />
            </ProtectedRoute>
          }
        />
        <Route
          path="/workshop"
          element={
            <ProtectedRoute allowedRoles={[...MANAGEMENT_ROLES, 'WORKSHOP_STAFF']}>
              <Workshop />
            </ProtectedRoute>
          }
        />
        <Route
          path="/billing"
          element={
            <ProtectedRoute allowedRoles={MANAGEMENT_ROLES}>
              <Billing />
            </ProtectedRoute>
          }
        />
        <Route
          path="/supplier-invoices"
          element={
            <ProtectedRoute allowedRoles={MANAGEMENT_ROLES}>
              <SupplierInvoices />
            </ProtectedRoute>
          }
        />
        <Route
          path="/suppliers"
          element={
            <ProtectedRoute allowedRoles={MANAGEMENT_ROLES}>
              <Suppliers />
            </ProtectedRoute>
          }
        />
        <Route
          path="/audit"
          element={
            <ProtectedRoute allowedRoles={['ADMIN', 'MANAGER']}>
              <AuditLog />
            </ProtectedRoute>
          }
        />
        {NAV_ITEMS.filter(
          (item) =>
            item.to !== '/' &&
            item.to !== '/clients' &&
            item.to !== '/vehicles' &&
            item.to !== '/workers' &&
            item.to !== '/jobs' &&
            item.to !== '/workshop' &&
            item.to !== '/billing' &&
            item.to !== '/supplier-invoices' &&
            item.to !== '/suppliers' &&
            item.to !== '/audit',
        ).map((item) => (
          <Route key={item.to} path={item.to} element={<NotImplemented />} />
        ))}
      </Route>
    </Routes>
  )
}

export default App
