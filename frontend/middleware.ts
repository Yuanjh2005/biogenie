import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

/**
 * 任意入口只要带 ticket，就在内部交给 /sso 做免登。
 * 先跳转到独立的 /sso 地址，避免免登页与目标页面共用路由缓存。
 */
export function middleware(request: NextRequest) {
  const ticket = request.nextUrl.searchParams.get("ticket")?.trim();
  if (!ticket) {
    return NextResponse.next();
  }
  if (request.nextUrl.pathname === "/sso") {
    return NextResponse.next();
  }

  const url = request.nextUrl.clone();
  url.pathname = "/sso";
  return NextResponse.redirect(url);
}

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico|images/|icon.png|api/).*)"],
};
