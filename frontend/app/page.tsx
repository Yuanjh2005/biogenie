import { redirect } from "next/navigation";

export default async function RootPage({
  searchParams,
}: {
  searchParams: Promise<{ ticket?: string }>;
}) {
  const params = await searchParams;
  const ticket = params.ticket?.trim();
  if (ticket) {
    redirect(`/sso?ticket=${encodeURIComponent(ticket)}`);
  }
  redirect("/home");
}
