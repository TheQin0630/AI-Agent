import { computed, type WritableComputedRef } from "vue";

export function useRouteFilter<T extends string>(
  options: readonly T[],
  readStatus: () => unknown,
  writeStatus: (status: T | undefined) => void,
): WritableComputedRef<T> {
  const fallback = options[0];
  if (!fallback) throw new Error("Route filter options cannot be empty");

  return computed({
    get() {
      const status = String(readStatus() ?? "") as T;
      return options.includes(status) ? status : fallback;
    },
    set(value) {
      writeStatus(value === fallback ? undefined : value);
    },
  });
}
