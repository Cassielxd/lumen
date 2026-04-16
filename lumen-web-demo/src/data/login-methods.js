export function normalizeLoginMethodOptions(items = []) {
  if (!Array.isArray(items)) {
    return [];
  }

  return items
    .map((item) => ({
      ...item,
      value: item?.value ?? item?.itemValue ?? "",
      label: item?.label ?? item?.value ?? item?.itemValue ?? ""
    }))
    .filter((item) => item.value);
}
