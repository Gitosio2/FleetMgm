import { act, create, type ReactTestRenderer } from 'react-test-renderer'
import { Modal } from 'react-native'
import { MobileMenu } from './MobileMenu'

;(globalThis as typeof globalThis & { IS_REACT_ACT_ENVIRONMENT: boolean }).IS_REACT_ACT_ENVIRONMENT = true

function renderMenu(): ReactTestRenderer {
  let screen: ReactTestRenderer | undefined

  act(() => {
    screen = create(<MobileMenu />)
  })

  if (!screen) {
    throw new Error('MobileMenu did not render')
  }

  return screen
}

describe('MobileMenu', () => {
  it('opens the navigation drawer from its accessible menu trigger', () => {
    const screen = renderMenu()

    expect(() => screen.root.findByProps({ accessibilityLabel: 'Cerrar menú' })).toThrow()
    expect(screen.root.findByProps({ accessibilityLabel: 'Abrir menú' }).props.accessibilityState?.expanded).toBe(false)

    act(() => screen.root.findByProps({ accessibilityLabel: 'Abrir menú' }).props.onPress())

    expect(screen.root.findByProps({ accessibilityLabel: 'Cerrar menú' })).toBeTruthy()
    expect(screen.root.findByProps({ accessibilityLabel: 'Menú de navegación' })).toBeTruthy()
    expect(screen.root.findByProps({ accessibilityLabel: 'Abrir menú' }).props.accessibilityState?.expanded).toBe(true)
    expect(screen.root.findByProps({ accessibilityLabel: 'Menú de navegación' }).props.accessibilityViewIsModal).toBe(true)
    expect(screen.root.findByProps({ accessibilityLabel: 'Opciones del menú' })).toBeTruthy()
  })

  it('closes the drawer from its close action and backdrop', () => {
    const screen = renderMenu()
    const open = () => act(() => screen.root.findByProps({ accessibilityLabel: 'Abrir menú' }).props.onPress())

    open()
    act(() => screen.root.findByProps({ accessibilityLabel: 'Cerrar menú' }).props.onPress())
    expect(() => screen.root.findByProps({ accessibilityLabel: 'Menú de navegación' })).toThrow()

    open()
    act(() => screen.root.findByProps({ accessibilityLabel: 'Cerrar menú desplegable' }).props.onPress())
    expect(() => screen.root.findByProps({ accessibilityLabel: 'Menú de navegación' })).toThrow()

    open()
    act(() => screen.root.findByType(Modal).props.onRequestClose())
    expect(() => screen.root.findByProps({ accessibilityLabel: 'Menú de navegación' })).toThrow()
  })

  it('shows the web navigation items in the established order without navigating', () => {
    const screen = renderMenu()
    act(() => screen.root.findByProps({ accessibilityLabel: 'Abrir menú' }).props.onPress())

    const menuItems = [...new Set(screen.root.findAllByProps({ accessibilityRole: 'button' })
      .filter((node) => node.props.accessibilityLabel?.startsWith('Ir a '))
      .map((node) => node.props.accessibilityLabel))]

    expect(menuItems).toEqual([
      'Ir a Panel',
      'Ir a Trabajos',
      'Ir a Agenda',
      'Ir a Órdenes de mantenimiento',
      'Ir a Facturación',
      'Ir a Gastos de proveedor',
      'Ir a Proveedores',
      'Ir a Vehículos',
      'Ir a Trabajadores',
      'Ir a Clientes',
      'Ir a Mapa GPS',
      'Ir a Registro de auditoría',
    ])
  })

  it('closes after an item selection without performing navigation', () => {
    const screen = renderMenu()
    act(() => screen.root.findByProps({ accessibilityLabel: 'Abrir menú' }).props.onPress())

    act(() => screen.root.findByProps({ accessibilityLabel: 'Ir a Trabajos' }).props.onPress())

    expect(() => screen.root.findByProps({ accessibilityLabel: 'Menú de navegación' })).toThrow()
  })
})
