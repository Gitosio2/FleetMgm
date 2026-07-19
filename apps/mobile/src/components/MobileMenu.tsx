import { useState, type ComponentType } from 'react'
import {
  Building2,
  CalendarClock,
  ClipboardList,
  Contact2,
  LayoutDashboard,
  MapPin,
  Menu,
  Receipt,
  ShieldCheck,
  Store,
  Truck,
  Users,
  Wrench,
  X,
} from 'lucide-react-native'
import { Modal, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native'

type MenuIcon = ComponentType<{ color?: string; size?: number; strokeWidth?: number }>

type MobileMenuItem = {
  icon: MenuIcon
  label: string
}

const MENU_ITEMS: MobileMenuItem[] = [
  { label: 'Panel', icon: LayoutDashboard },
  { label: 'Trabajos', icon: ClipboardList },
  { label: 'Agenda', icon: CalendarClock },
  { label: 'Órdenes de mantenimiento', icon: Wrench },
  { label: 'Facturación', icon: Receipt },
  { label: 'Gastos de proveedor', icon: Store },
  { label: 'Proveedores', icon: Contact2 },
  { label: 'Vehículos', icon: Truck },
  { label: 'Trabajadores', icon: Users },
  { label: 'Clientes', icon: Building2 },
  { label: 'Mapa GPS', icon: MapPin },
  { label: 'Registro de auditoría', icon: ShieldCheck },
]

export function MobileMenu() {
  const [isOpen, setIsOpen] = useState(false)

  function closeMenu() {
    setIsOpen(false)
  }

  return (
    <View style={styles.container}>
      <Pressable
        accessibilityLabel="Abrir menú"
        accessibilityRole="button"
        accessibilityState={{ expanded: isOpen }}
        onPress={() => setIsOpen(true)}
        style={({ pressed }) => [styles.trigger, pressed && styles.pressed]}
      >
        <Menu color="#0f172a" size={24} strokeWidth={2} />
      </Pressable>

      <Modal
        animationType="fade"
        onRequestClose={closeMenu}
        statusBarTranslucent
        transparent
        visible={isOpen}
      >
        {isOpen && (
          <View accessibilityViewIsModal importantForAccessibility="yes" style={styles.overlay}>
            <Pressable
              accessibilityLabel="Cerrar menú desplegable"
              accessibilityRole="button"
              onPress={closeMenu}
              style={styles.backdrop}
            />
            <View
              accessibilityLabel="Menú de navegación"
              accessibilityRole="menu"
              accessibilityViewIsModal
              importantForAccessibility="yes"
              style={styles.drawer}
            >
              <View style={styles.header}>
                <View style={styles.logoBadge}>
                  <Truck color="#2563eb" size={22} strokeWidth={2.25} />
                </View>
                <Text style={styles.brand}>FleetMgm</Text>
                <Pressable
                  accessibilityLabel="Cerrar menú"
                  accessibilityRole="button"
                  onPress={closeMenu}
                  style={({ pressed }) => [styles.closeButton, pressed && styles.pressed]}
                >
                  <X color="#475569" size={22} strokeWidth={2} />
                </Pressable>
              </View>

              <ScrollView accessibilityLabel="Opciones del menú" contentContainerStyle={styles.menuList}>
                {MENU_ITEMS.map(({ icon: Icon, label }) => (
                  <Pressable
                    key={label}
                    accessibilityLabel={`Ir a ${label}`}
                    accessibilityRole="button"
                    onPress={closeMenu}
                    style={({ pressed }) => [styles.menuItem, pressed && styles.menuItemPressed]}
                  >
                    <Icon color="#475569" size={21} strokeWidth={2} />
                    <Text style={styles.menuItemText}>{label}</Text>
                  </Pressable>
                ))}
              </ScrollView>

              <View style={styles.footer}>
                <Text style={styles.footerText}>Selecciona una sección para continuar.</Text>
              </View>
            </View>
          </View>
        )}
      </Modal>
    </View>
  )
}

const styles = StyleSheet.create({
  backdrop: {
    bottom: 0,
    left: 0,
    position: 'absolute',
    right: 0,
    top: 0,
  },
  brand: {
    color: '#0f172a',
    flex: 1,
    fontSize: 18,
    fontWeight: '700',
  },
  closeButton: {
    alignItems: 'center',
    borderRadius: 20,
    height: 40,
    justifyContent: 'center',
    width: 40,
  },
  container: {
    zIndex: 10,
  },
  drawer: {
    backgroundColor: '#ffffff',
    bottom: 0,
    elevation: 8,
    left: 0,
    paddingBottom: 20,
    paddingHorizontal: 14,
    paddingTop: 20,
    position: 'absolute',
    top: 0,
    width: 268,
  },
  footer: {
    borderTopColor: '#e2e8f0',
    borderTopWidth: 1,
    paddingHorizontal: 6,
    paddingTop: 12,
  },
  footerText: {
    color: '#64748b',
    fontSize: 11,
    lineHeight: 16,
  },
  header: {
    alignItems: 'center',
    flexDirection: 'row',
    gap: 10,
    paddingBottom: 16,
    paddingHorizontal: 6,
  },
  logoBadge: {
    alignItems: 'center',
    backgroundColor: '#dbeafe',
    borderRadius: 10,
    height: 36,
    justifyContent: 'center',
    width: 36,
  },
  menuItem: {
    alignItems: 'center',
    borderRadius: 10,
    flexDirection: 'row',
    gap: 14,
    minHeight: 48,
    paddingHorizontal: 12,
  },
  menuItemPressed: {
    backgroundColor: '#eff6ff',
  },
  menuItemText: {
    color: '#334155',
    fontSize: 15,
    fontWeight: '500',
  },
  menuList: {
    gap: 4,
    paddingBottom: 16,
  },
  overlay: {
    backgroundColor: 'rgba(0, 0, 0, 0.45)',
    bottom: 0,
    left: 0,
    position: 'absolute',
    right: 0,
    top: 0,
  },
  pressed: {
    backgroundColor: '#f1f5f9',
  },
  trigger: {
    alignItems: 'center',
    borderRadius: 20,
    height: 40,
    justifyContent: 'center',
    width: 40,
  },
})
