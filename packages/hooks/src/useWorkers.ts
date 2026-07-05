import type { CreateWorkerRequest, UpdateWorkerRequest, Worker } from '@fleetmgm/api'
import { createCrudHooks } from './createCrudHooks'

const workerHooks = createCrudHooks<Worker, CreateWorkerRequest, UpdateWorkerRequest>(
  'workers',
  '/workers',
)

export const useWorkers = workerHooks.useList
export const useCreateWorker = workerHooks.useCreate
export const useUpdateWorker = workerHooks.useUpdate
export const useDeleteWorker = workerHooks.useDelete
