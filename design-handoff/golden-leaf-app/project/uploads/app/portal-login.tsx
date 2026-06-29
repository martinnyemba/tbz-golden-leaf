import React from "react";
import { useLocalSearchParams } from "expo-router";

import { PortalLoginForm } from "@/components/PortalLoginForm";

export default function PortalLoginScreen() {
  const { returnTo } = useLocalSearchParams() as { returnTo?: string };
  return <PortalLoginForm returnTo={returnTo} />;
}

