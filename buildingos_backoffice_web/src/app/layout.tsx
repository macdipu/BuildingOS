import type { Metadata } from "next";
import localFont from "next/font/local";
import "./globals.css";

// Bundled OFL fonts (same files as user_app) so builds need no network.
const display = localFont({
  variable: "--nf-display",
  src: [
    { path: "../fonts/PlusJakartaSans-SemiBold.ttf", weight: "600" },
    { path: "../fonts/PlusJakartaSans-Bold.ttf", weight: "700" },
  ],
});

const body = localFont({
  variable: "--nf-body",
  src: [
    { path: "../fonts/Inter-Regular.ttf", weight: "400" },
    { path: "../fonts/Inter-Medium.ttf", weight: "500" },
    { path: "../fonts/Inter-SemiBold.ttf", weight: "600" },
    { path: "../fonts/Inter-Bold.ttf", weight: "700" },
  ],
});

export const metadata: Metadata = {
  title: "BuildingOS Back Office",
  description: "BuildingOS platform back-office console",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="en" className={`${display.variable} ${body.variable} h-full antialiased`}>
      <body className="min-h-full">{children}</body>
    </html>
  );
}
