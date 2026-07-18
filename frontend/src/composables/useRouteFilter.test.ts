import { ref } from "vue";
import { describe, expect, it } from "vitest";
import { useRouteFilter } from "./useRouteFilter";

describe("useRouteFilter", () => {
  it("follows route status when navigating between procurement and todo center", () => {
    const routeStatus = ref<string>();
    const filter = useRouteFilter(
      ["全部", "待确认", "已确认"],
      () => routeStatus.value,
      (status) => {
        routeStatus.value = status;
      },
    );

    expect(filter.value).toBe("全部");

    routeStatus.value = "待确认";
    expect(filter.value).toBe("待确认");

    filter.value = "全部";
    expect(routeStatus.value).toBeUndefined();
    expect(filter.value).toBe("全部");
  });

  it("falls back to all for unsupported route filters", () => {
    const routeStatus = ref<string | undefined>("不存在的状态");
    const filter = useRouteFilter(
      ["全部", "待确认", "已确认"],
      () => routeStatus.value,
      (status) => {
        routeStatus.value = status;
      },
    );

    expect(filter.value).toBe("全部");
  });
});
