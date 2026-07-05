import { Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from '@/components/auth/ProtectedRoute'
import { Layout } from '@/components/layout/Layout'
import { Login } from '@/pages/Login'
import { Dashboard } from '@/pages/Dashboard'
import { Clients } from '@/pages/Clients'
import { Vehicles } from '@/pages/Vehicles'
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
        {NAV_ITEMS.filter(
          (item) => item.to !== '/' && item.to !== '/clients' && item.to !== '/vehicles',
        ).map((item) => (
          <Route key={item.to} path={item.to} element={<NotImplemented />} />
        ))}
      </Route>
    </Routes>
  )
}

export default App
