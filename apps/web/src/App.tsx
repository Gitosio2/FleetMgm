import { Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from '@/components/auth/ProtectedRoute'
import { Layout } from '@/components/layout/Layout'
import { Login } from '@/pages/Login'
import { Dashboard } from '@/pages/Dashboard'
import { NotImplemented } from '@/pages/NotImplemented'
import { NAV_ITEMS } from '@/components/layout/nav-items'

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
        {NAV_ITEMS.filter((item) => item.to !== '/').map((item) => (
          <Route key={item.to} path={item.to} element={<NotImplemented />} />
        ))}
      </Route>
    </Routes>
  )
}

export default App
