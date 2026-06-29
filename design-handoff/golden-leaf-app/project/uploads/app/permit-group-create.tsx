import React from "react";

import PermitGroupScreen from "./permit-group";

export default function PermitGroupCreateRoute() {
  return <PermitGroupScreen initialMode="create_header" showDashboard={false} showStatusPills={false} exitMode="back" listSubtitle="New Group Permit" />;
}

