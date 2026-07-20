/** URL is available at runtime in React Native, without requiring DOM types. */
declare class URL {
  constructor(url: string)
  hostname: string
  protocol: string
  toString(): string
}
