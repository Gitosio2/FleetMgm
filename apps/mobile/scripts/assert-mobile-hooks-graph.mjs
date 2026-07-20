import { dirname, relative, resolve, sep } from 'node:path'
import { fileURLToPath } from 'node:url'
import ts from 'typescript'

const appDirectory = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const projectDirectory = resolve(appDirectory, '..', '..')
const hooksSourceDirectory = resolve(projectDirectory, 'packages', 'hooks', 'src')
const configPath = resolve(appDirectory, 'tsconfig.typecheck.json')
const config = ts.readConfigFile(configPath, ts.sys.readFile)

if (config.error) {
  throw new Error(ts.flattenDiagnosticMessageText(config.error.messageText, '\n'))
}

const parsedConfig = ts.parseJsonConfigFileContent(config.config, ts.sys, appDirectory)
const program = ts.createProgram({
  options: parsedConfig.options,
  rootNames: parsedConfig.fileNames,
})
const hookSourcePrefix = `${hooksSourceDirectory}${sep}`
const allowedHookSources = new Set([
  'packages/hooks/src/mobile.ts',
  'packages/hooks/src/useAuth.ts',
])
const loadedHookSources = program.getSourceFiles()
  .map((sourceFile) => sourceFile.fileName)
  .filter((fileName) => fileName.startsWith(hookSourcePrefix))
  .map((fileName) => relative(projectDirectory, fileName).replaceAll(sep, '/'))

const unsafeHookSources = loadedHookSources.filter(
  (fileName) => fileName && !allowedHookSources.has(fileName),
)

if (unsafeHookSources.length > 0) {
  throw new Error(
    `Mobile typecheck must not load web hooks: ${unsafeHookSources.join(', ')}`,
  )
}
