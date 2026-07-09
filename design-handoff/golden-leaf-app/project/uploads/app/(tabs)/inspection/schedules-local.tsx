import { useFocusEffect, useRouter } from "expo-router";
import { useCallback, useMemo, useState } from "react";
import { Alert, FlatList, Pressable, StyleSheet, View } from "react-native";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";

import { ThemedText } from "@/components/themed-text";
import { ThemedView } from "@/components/themed-view";
import { PrimaryButton } from "@/components/ui/form-controls";
import { ConflictCard } from "@/components/ConflictCard";
import { Colors } from "@/constants/theme";
import { useColorScheme } from "@/hooks/use-color-scheme";
import { FontFamily, FontWeight } from "@/constants/typography";
import {
  listScheduledInspections,
  syncScheduledInspections,
  syncSingleScheduledInspection,
  deleteScheduledInspection,
  type ScheduledInspectionDraft,
} from "@/lib/inspection-storage";
import { useNetworkStatus } from "@/lib/network-state";

function inspectionTypeLabel(code: string) {
  const c = (code ?? "").trim().toUpperCase();
  if (c === "GROWER_VALIDATION") return "Grower Validation";
  if (c === "NURSERY_INSPECTION") return "Nursery Inspection";
  if (c === "FIELD_INSPECTION") return "Field Inspection";
  if (c === "CURING_INSPECTION") return "Curing Inspection";
  return "Inspection";
}

function provinceLabel(code: string) {
  const c = (code ?? "").trim().toUpperCase();
  if (c === "NORTH_WESTERN") return "North-Western";
  if (!c) return "-";
  return c[0] + c.slice(1).toLowerCase().replace(/_/g, " ");
}

const STATUS_COLOR: Record<ScheduledInspectionDraft["syncStatus"], string> = {
  pending: "#D97706",
  synced: "#0B6B3A",
  failed: "#DC2626",
  needs_review: "#92400E",
};

export default function OfflineSchedulesScreen() {
  const router = useRouter();
  const theme = (useColorScheme() ?? "light") === "dark" ? "dark" : "light";
  const { isConnected } = useNetworkStatus();
  const isOffline = isConnected === false;

  const [schedules, setSchedules] = useState<ScheduledInspectionDraft[]>([]);
  const [syncingId, setSyncingId] = useState<string | null>(null);
  const [syncingAll, setSyncingAll] = useState(false);

  const refresh = useCallback(async () => {
    const all = await listScheduledInspections();
    setSchedules(all);
  }, []);

  useFocusEffect(useCallback(() => { refresh(); }, [refresh]));

  const pendingCount = useMemo(
    () => schedules.filter((r) => r.syncStatus === "pending" || r.syncStatus === "failed").length,
    [schedules],
  );
  const needsReviewCount = useMemo(
    () => schedules.filter((r) => r.syncStatus === "needs_review").length,
    [schedules],
  );

  async function promptDelete(id: string, label: string) {
    Alert.alert(
      "Delete Offline Schedule",
      `Permanently delete the offline schedule for ${label}?`,
      [
        { text: "Cancel", style: "cancel" },
        { text: "Delete", style: "destructive", onPress: async () => { await deleteScheduledInspection(id); refresh(); } },
      ],
    );
  }

  function navigateToEdit(item: ScheduledInspectionDraft) {
    const p = item.payload;
    router.push({
      pathname: "/inspection/schedule",
      params: {
        draftId: item.id,
        returnTo: "/inspection/schedules-local",
        growerId: p.growerId,
        inspectorId: p.inspectorId,
        inspectionType: p.inspectionType,
        scheduledDate: p.scheduledDate,
        province: p.province,
        district: p.district,
        notes: p.notes,
      } as any,
    });
  }

  async function syncAll() {
    if (isOffline) { Alert.alert("Offline", "Connect to the internet to sync schedules."); return; }
    setSyncingAll(true);
    try {
      const res = await syncScheduledInspections();
      await refresh();
      if (res.requiresLogin) {
        Alert.alert("Login required", "Login to sync saved schedules.", [
          { text: "Login", onPress: () => router.push({ pathname: "/portal-login", params: { returnTo: "/inspection/schedules-local" } } as any) },
          { text: "OK" },
        ]);
        return;
      }
      if (res.failedCount === 0) {
        Alert.alert("Sync complete", `${res.syncedCount} schedule(s) synced successfully.`);
      } else {
        Alert.alert("Partial sync", `${res.syncedCount} synced, ${res.failedCount} failed.\nCheck failed cards for details.`);
      }
    } finally {
      setSyncingAll(false);
    }
  }

  async function syncOne(item: ScheduledInspectionDraft) {
    if (isOffline) { Alert.alert("Offline", "Connect to the internet to sync this schedule."); return; }
    if (syncingAll || syncingId) return;
    setSyncingId(item.id);
    try {
      const res = await syncSingleScheduledInspection(item.id);
      await refresh();
      if (res.requiresLogin) {
        Alert.alert("Login required", "Login to sync this schedule.");
      } else if (!res.success) {
        // Reload to get conflictDetail
        const all = await listScheduledInspections();
        const updated = all.find((s) => s.id === item.id);
        if (updated?.conflictDetail) {
          Alert.alert(updated.conflictDetail.summary, updated.conflictDetail.advice);
        } else {
          Alert.alert("Sync failed", res.error || "An unknown error occurred.");
        }
      } else {
        Alert.alert("Success", "Schedule synced successfully.");
      }
    } finally {
      setSyncingId(null);
    }
  }

  return (
    <ThemedView style={styles.container}>
      <View style={styles.header}>
        {isOffline && (
          <View style={styles.offlineBanner}>
            <MaterialIcons name="wifi-off" size={14} color="#FFFFFF" />
            <ThemedText style={styles.offlineBannerText}>Offline — sync when connected</ThemedText>
          </View>
        )}
        <ThemedText style={styles.countLine}>
          Pending: {pendingCount} · Needs review: {needsReviewCount} · Total: {schedules.length}
        </ThemedText>
        <PrimaryButton
          title={syncingAll ? "Syncing…" : `Sync Pending (${pendingCount})`}
          disabled={pendingCount === 0 || syncingAll || Boolean(syncingId) || isOffline}
          onPress={syncAll}
        />
      </View>

      <FlatList
        data={schedules}
        keyExtractor={(item) => item.id}
        contentContainerStyle={styles.list}
        renderItem={({ item }) => {
          const borderColor = STATUS_COLOR[item.syncStatus];
          const isSynced = item.syncStatus === "synced";
          const needsAttention = item.syncStatus === "failed" || item.syncStatus === "needs_review";

          return (
            <View style={[styles.card, { borderLeftColor: borderColor }]}>
              {/* Header */}
              <View style={styles.rowTop}>
                <Pressable style={{ flex: 1 }} onPress={() => navigateToEdit(item)}>
                  <ThemedText style={[styles.typeLabel, { flexShrink: 1 }]} numberOfLines={1} ellipsizeMode="tail">
                    {inspectionTypeLabel(item.payload.inspectionType)}
                  </ThemedText>
                </Pressable>
                <View style={[styles.statusBadge, { backgroundColor: borderColor + "18", borderColor: borderColor + "60" }]}>
                  <View style={[styles.statusDot, { backgroundColor: borderColor }]} />
                  <ThemedText style={[styles.statusText, { color: borderColor }]}>
                    {item.syncStatus.toUpperCase()}
                  </ThemedText>
                </View>
              </View>

              {/* Details */}
              <Pressable onPress={() => navigateToEdit(item)}>
                <ThemedText style={styles.subline} numberOfLines={1} ellipsizeMode="tail">
                  {provinceLabel(item.payload.province)} · {item.payload.district}
                </ThemedText>
                <ThemedText style={styles.subline} numberOfLines={1} ellipsizeMode="tail">
                  Scheduled: {item.payload.scheduledDate}
                </ThemedText>
                {item.payload.notes ? (
                  <ThemedText style={styles.notes} numberOfLines={1}>Notes: {item.payload.notes}</ThemedText>
                ) : null}
              </Pressable>

              {/* Conflict advice */}
              {needsAttention && item.conflictDetail && (
                <ConflictCard conflict={item.conflictDetail} />
              )}

              {/* Plain error fallback */}
              {needsAttention && !item.conflictDetail && item.lastError ? (
                <View style={styles.plainError}>
                  <MaterialIcons name="error-outline" size={13} color="#DC2626" />
                  <ThemedText style={styles.plainErrorText} numberOfLines={3}>{item.lastError}</ThemedText>
                </View>
              ) : null}

              {/* Actions */}
              {!isSynced && (
                <View style={styles.actions}>
                  <Pressable style={styles.editBtn} onPress={() => navigateToEdit(item)}>
                    <MaterialIcons name="edit" size={14} color="#0B6B3A" />
                    <ThemedText style={styles.editBtnText} numberOfLines={1} ellipsizeMode="tail">
                      Edit
                    </ThemedText>
                  </Pressable>

                  <Pressable
                    style={styles.deleteBtn}
                    onPress={() => promptDelete(item.id, inspectionTypeLabel(item.payload.inspectionType))}
                  >
                    <MaterialIcons name="delete-outline" size={15} color="#DC2626" />
                  </Pressable>

                  <Pressable
                    style={[styles.syncBtn, (syncingId === item.id || syncingAll || isOffline) && styles.syncBtnDisabled]}
                    onPress={() => syncOne(item)}
                    disabled={syncingId === item.id || syncingAll || isOffline}
                  >
                    <MaterialIcons
                      name={syncingId === item.id ? "sync" : "cloud-upload"}
                      size={14}
                      color={syncingId === item.id || isOffline ? "#9CA3AF" : Colors[theme].primary}
                    />
                    <ThemedText
                      style={[styles.syncBtnText, (syncingId === item.id || isOffline) && { color: "#9CA3AF" }]}
                      numberOfLines={1}
                      ellipsizeMode="tail"
                    >
                      {syncingId === item.id ? "Syncing…" : "Sync"}
                    </ThemedText>
                  </Pressable>
                </View>
              )}
            </View>
          );
        }}
        ListEmptyComponent={
          <View style={[styles.empty, { borderColor: Colors[theme].border, backgroundColor: Colors[theme].surface }]}>
            <ThemedText type="defaultSemiBold">No offline schedules</ThemedText>
            <ThemedText>Submit an inspection schedule while offline to see it here.</ThemedText>
          </View>
        }
      />
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: { padding: 16, gap: 10 },
  offlineBanner: { flexDirection: "row", alignItems: "center", gap: 6, backgroundColor: "#374151", borderRadius: 8, paddingHorizontal: 12, paddingVertical: 8 },
  offlineBannerText: { color: "#FFFFFF", fontSize: 12, flex: 1 },
  countLine: { fontSize: 13, color: "#6B7280" },
  list: { paddingHorizontal: 16, paddingBottom: 30, gap: 10 },
  card: {
    backgroundColor: "#FFFFFF",
    borderRadius: 12,
    padding: 14,
    gap: 6,
    borderWidth: 1,
    borderColor: "#E5E7EB",
    borderLeftWidth: 4,
    overflow: "hidden",
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.05,
    shadowRadius: 4,
    elevation: 2,
  },
  rowTop: { flexDirection: "row", alignItems: "center", justifyContent: "space-between", gap: 8 },
  typeLabel: { fontSize: 14, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold, color: "#111827" },
  statusBadge: { flexDirection: "row", alignItems: "center", gap: 4, paddingHorizontal: 8, paddingVertical: 3, borderRadius: 999, borderWidth: 1 },
  statusDot: { width: 5, height: 5, borderRadius: 3 },
  statusText: { fontSize: 10, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold },
  subline: { fontSize: 12, color: "#6B7280" },
  notes: { fontSize: 11, color: "#9CA3AF", marginTop: 2 },
  plainError: { flexDirection: "row", alignItems: "flex-start", gap: 5, backgroundColor: "#FEF2F2", borderRadius: 6, padding: 8, borderWidth: 1, borderColor: "#FECACA" },
  plainErrorText: { flex: 1, fontSize: 11, color: "#DC2626" },
  actions: { flexDirection: "row", gap: 8, marginTop: 4, justifyContent: "flex-end" },
  editBtn: { flexDirection: "row", alignItems: "center", gap: 4, paddingHorizontal: 10, paddingVertical: 7, borderRadius: 8, borderWidth: 1, borderColor: "#0B6B3A", backgroundColor: "#F0F9F4" },
  editBtnText: { fontSize: 12, color: "#0B6B3A", fontFamily: FontFamily.serif, fontWeight: FontWeight.medium },
  deleteBtn: { width: 36, height: 36, borderRadius: 8, alignItems: "center", justifyContent: "center", borderWidth: 1, borderColor: "#FECACA", backgroundColor: "#FEF2F2" },
  syncBtn: { flexDirection: "row", alignItems: "center", gap: 5, paddingHorizontal: 12, paddingVertical: 7, borderRadius: 8, borderWidth: 1, borderColor: "#D1D5DB", backgroundColor: "#F9FAFB" },
  syncBtnDisabled: { borderColor: "#E5E7EB" },
  syncBtnText: { fontSize: 12, color: "#374151", fontFamily: FontFamily.serif, fontWeight: FontWeight.medium },
  empty: { borderRadius: 16, padding: 20, gap: 6, marginTop: 20, alignItems: "center" },
});
