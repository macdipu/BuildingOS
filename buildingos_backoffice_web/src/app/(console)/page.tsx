import { redirect } from "next/navigation";
import { DashboardScreen } from "@/components/DashboardScreen";
import { currentSession } from "@/lib/session";

export default async function DashboardPage() {
  const session = await currentSession();
  if (!session) redirect("/login");
  return <DashboardScreen roles={session.roles} />;
}
