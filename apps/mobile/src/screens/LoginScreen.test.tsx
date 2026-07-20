import { act, create, type ReactTestRenderer } from 'react-test-renderer'
import { useLogin } from '@fleetmgm/hooks/mobile'
import { LoginScreen } from './LoginScreen'

jest.mock('@fleetmgm/hooks/mobile', () => ({ useLogin: jest.fn() }))

const mockedUseLogin = jest.mocked(useLogin)

;(globalThis as typeof globalThis & { IS_REACT_ACT_ENVIRONMENT: boolean }).IS_REACT_ACT_ENVIRONMENT = true

function renderLoginScreen(): ReactTestRenderer {
  let screen: ReactTestRenderer | undefined

  act(() => {
    screen = create(<LoginScreen />)
  })

  if (!screen) {
    throw new Error('LoginScreen did not render')
  }

  return screen
}

describe('LoginScreen', () => {
  beforeEach(() => {
    mockedUseLogin.mockReturnValue({
      isError: false,
      isPending: false,
      mutate: (_request: unknown, options?: { onSuccess?: () => void }) => options?.onSuccess?.(),
    } as never)
  })

  it('submits credentials through the shared login hook', () => {
    const mutate = jest.fn((
      _request: unknown,
    ) => undefined)
    mockedUseLogin.mockReturnValue({ isError: false, isPending: false, mutate } as never)

    const screen = renderLoginScreen()
    const emailInput = screen.root.findByProps({ accessibilityLabel: 'Correo electrónico' })
    const passwordInput = screen.root.findByProps({ accessibilityLabel: 'Contraseña' })
    const submitButton = screen.root.findByProps({
      accessibilityRole: 'button',
      accessibilityLabel: 'Iniciar sesión',
    })

    act(() => {
      emailInput.props.onChangeText('admin@fleetmgm.demo')
      passwordInput.props.onChangeText('Demo1234!')
    })
    act(() => submitButton.props.onPress())

    expect(mutate).toHaveBeenCalledWith({ email: 'admin@fleetmgm.demo', password: 'Demo1234!' })
  })

  it('keeps the submission disabled until both credentials are present', () => {
    const screen = renderLoginScreen()
    const emailInput = screen.root.findByProps({ accessibilityLabel: 'Correo electrónico' })
    const passwordInput = screen.root.findByProps({ accessibilityLabel: 'Contraseña' })
    const submitButton = () => screen.root.findByProps({
      accessibilityRole: 'button',
      accessibilityLabel: 'Iniciar sesión',
    })

    expect(submitButton().props.accessibilityState?.disabled).toBe(true)

    act(() => {
      emailInput.props.onChangeText('admin@fleetmgm.demo')
      passwordInput.props.onChangeText('Demo1234!')
    })

    expect(submitButton().props.accessibilityState?.disabled).toBe(false)
  })

  it('disables submission and announces progress while login is pending', () => {
    const mutate = jest.fn()
    const screen = renderLoginScreen()
    const emailInput = screen.root.findByProps({ accessibilityLabel: 'Correo electrónico' })
    const passwordInput = screen.root.findByProps({ accessibilityLabel: 'Contraseña' })

    act(() => {
      emailInput.props.onChangeText('admin@fleetmgm.demo')
      passwordInput.props.onChangeText('Demo1234!')
    })

    mockedUseLogin.mockReturnValue({
      isError: false,
      isPending: true,
      mutate,
    } as never)
    act(() => screen.update(<LoginScreen />))

    const submitButton = screen.root.findByProps({
      accessibilityRole: 'button',
      accessibilityLabel: 'Iniciar sesión',
    })

    expect(submitButton.props.disabled).toBe(true)
    expect(submitButton.props.accessibilityState?.disabled).toBe(true)
    expect(screen.root.findByProps({ accessibilityLabel: 'Iniciando sesión' })).toBeTruthy()

    act(() => submitButton.props.onPress())

    expect(mutate).not.toHaveBeenCalled()
  })

  it('shows a generic error only for rejected credentials', () => {
    mockedUseLogin.mockReturnValue({
      error: { isAxiosError: true, response: { status: 401 } },
      isError: true,
      isPending: false,
      mutate: jest.fn(),
    } as never)

    const screen = renderLoginScreen()

    expect(screen.root.findByProps({ children: 'Credenciales inválidas' })).toBeTruthy()
  })

  it('shows a connection error when the request cannot reach the server', () => {
    mockedUseLogin.mockReturnValue({
      error: new Error('Network Error'),
      isError: true,
      isPending: false,
      mutate: jest.fn(),
    } as never)

    const screen = renderLoginScreen()

    expect(screen.root.findByProps({ children: 'No se pudo conectar al servidor. Inténtalo de nuevo.' })).toBeTruthy()
  })

  it('shows a connection error when the server fails unexpectedly', () => {
    mockedUseLogin.mockReturnValue({
      error: { isAxiosError: true, response: { status: 500 } },
      isError: true,
      isPending: false,
      mutate: jest.fn(),
    } as never)

    const screen = renderLoginScreen()

    expect(screen.root.findByProps({ children: 'No se pudo conectar al servidor. Inténtalo de nuevo.' })).toBeTruthy()
  })
})
