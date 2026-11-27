export const PHONE_RE = /^1[3-9]\d{9}$/;
export const CODE_RE = /^\d{6}$/;

export function isValidPhone(phone: string): boolean {
  return PHONE_RE.test(phone.trim());
}

export function isValidCode(code: string): boolean {
  return CODE_RE.test(code.trim());
}

/** 手机号掩码显示，如 13312345678 -> 133****5678 */
export function maskPhone(phone?: string | null): string {
  if (!phone || phone.length < 7) return phone ?? "";
  return `${phone.slice(0, 3)}****${phone.slice(-4)}`;
}
