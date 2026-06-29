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
  listInspectionReports,
  syncPendingReports,
  deleteInspectionReport,
  extractErrorMessage,
  type InspectionReport,
} from "@/lib/inspection-storage";
import { useNetworkStatus } from "@/lib/network-state";

const STATUS_COLOR: Record<InspectionReport["syncStatus"], string> = {
  pending: "#D97706",
  synced: "#0B6B3A",
  failed: "#DC2626",
  needs_review: "#92400E",
};

const STAGE_ICON: Record<string, string> = {
  "Nursery": "eco",
  "Field": "grass",
  "Curing": "whatshot",
  "Grower Validation": "verified-user",
};

export default function InspectionReportsScreen() {
  const router = useRouter();
  const theme = (useColorScheme() ?? "light") === "dark" ? "dark" : "light";
  const { isConnected } = useNetworkStatus();
  const isOffline = isConnected === false;

  const [reports, setReports] = useState<InspectionReport[]>([]);
  const [syncingAll, setSyncingAll] = useState(false);
  const [syncingId, setSyncingId] = useState("");

  const refresh = useCallback(async () => {
    const all = await listInspectionReports();
    setReports(all);
  }, []);

  useFocusEffect(useCallback(() => { refresh(); }, [refresh]));

  const pendingCount = useMemo(
    () => reports.filter((r) => r.syncStatus === "pending" || r.syncStatus === "failed").length,
    [reports],
  );
  const failedCount = useMemo(
    () => reports.filter((r) => r.syncStatus === "failed").length,
    [reports],
  );
  const needsReviewCount = useMemo(
    () => reports.filter((r) => r.syncStatus === "needs_review").length,
    [reports],
  );

  async function promptDelete(id: string, name: string) {
    Alert.alert(
      "Delete Offline Report",
      `Permanently delete the offline inspection report for ${name}?`,
      [
        { text: "Cancel", style: "cancel" },
        { text: "Delete", style: "destructive", onPress: async () => { await deleteInspectionReport(id); refresh(); } },
      ],
    );
  }

  async function syncAll() {
    if (isOffline) { Alert.alert("Offline", "Connect to the internet to sync inspection reports."); return; }
    setSyncingAll(true);
    try {
      const res = await syncPendingReports();
      await refresh();
      if (res.requiresLogin) {
        Alert.alert("Login required", "Login to sync saved inspections to the web portal.", [
          { text: "Login", onPress: () => router.push({ pathname: "/portal-login", params: { returnTo: "/inspection/reports" } } as any) },
          { text: "OK" },
        ]);
        return;
      }
      if (res.failedCount === 0) {
        Alert.alert("Sync complete", `${res.syncedCount} report(s) uploaded successfully.`);
      } else {
        Alert.alert(
          "Partial sync",
          `${res.syncedCount} synced, ${res.failedCount} failed.\nCheck each failed card below for guidance.`,
        );
      }
    } finally {
      setSyncingAll(false);
    }
  }

  async function syncOne(id: string) {
    if (isOffline) { Alert.alert("Offline", "Connect to the internet to sync this report."); return; }
    if (syncingAll || syncingId) return;
    setSyncingId(id);
    try {
      const res = await syncPendingReports([id]);
      await refresh();
      if (res.requiresLogin) {
        Alert.alert("Login required", "Login to sync this inspection.", [
          { text: "Login", onPress: () => router.push({ pathname: "/portal-login", params: { returnTo: "/inspection/reports" } } as any) },
          { text: "OK" },
        ]);
        return;
      }
      const failed = (res.failures ?? [])[0];
      if (failed?.lastError) {
        // If we have a conflict detail, show structured advice
        const current = (await listInspectionReports()).find((r) => r.id === id);
        if (current?.conflictDetail) {
          Alert.alert(current.conflictDetail.summary, current.conflictDetail.advice);
        } else {
          Alert.alert("Sync failed", extractErrorMessage(failed.lastError));
        }
        return;
      }
      Alert.alert("Success", "Report synced successfully.");
    } finally {
      setSyncingId("");
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
          Pending: {pendingCount} · Failed: {failedCount} · Needs review: {needsReviewCount} · Total: {reports.length}
        </ThemedText>
        <PrimaryButton
          title={syncingAll ? "Syncing…" : `Sync Pending (${pendingCount})`}
          disabled={pendingCount === 0 || syncingAll || Boolean(syncingId) || isOffline}
          onPress={syncAll}
        />
      </View>

      <FlatList
        data={reports}
        keyExtractor={(item) => item.id}
        contentContainerStyle={styles.list}
        renderItem={({ item }) => {
          const borderColor = STATUS_COLOR[item.syncStatus];
          const isSynced = item.syncStatus === "synced";
          const needsAttention = item.syncStatus === "failed" || item.syncStatus === "needs_review";
          const icon = STAGE_ICON[item.stage] ?? "assignment";

          return (
            <View style={[styles.card, { borderLeftColor: borderColor }]}>
              {/* Header: stage icon + name + status badge */}
              <View style={styles.rowTop}>
                <View style={styles.stagePill}>
                  <MaterialIcons name={icon as any} size={14} color="#0B6B3A" />
                  <ThemedText style={styles.stageLabel}>{item.stage}</ThemedText>
                </View>
                <View style={[styles.statusBadge, { backgroundColor: borderColor + "18", borderColor: borderColor + "60" }]}>
                  <View style={[styles.statusDot, { backgroundColor: borderColor }]} />
                  <ThemedText style={[styles.statusText, { color: borderColor }]}>
                    {item.syncStatus.toUpperCase()}
                  </ThemedText>
                </View>
              </View>

              {/* Grower + timestamp */}
              <ThemedText style={styles.growerLine} numberOfLines={1} ellipsizeMode="tail">
                {item.grower.name}
                {item.grower.growerId ? ` · ${item.grower.growerId}` : ""}
              </ThemedText>
              <ThemedText style={styles.timestamp}>
                {new Date(item.audit.submittedAt).toLocaleString()}
              </ThemedText>

              {/* Conflict advice card */}
              {needsAttention && item.conflictDetail && (
                <ConflictCard conflict={item.conflictDetail} />
              )}

              {/* Plain error fallback */}
              {needsAttention && !item.conflictDetail && item.lastError ? (
                <View style={styles.plainError}>
                  <MaterialIcons name="error-outline" size={13} color="#DC2626" />
                  <ThemedText style={styles.plainErrorText} numberOfLines={3}>
                    {extractErrorMessage(item.lastError)}
                  </ThemedText>
                </View>
              ) : null}

              {/* Actions */}
              <View style={styles.actions}>
                {!isSynced && (
                  <Pressable
                    style={styles.editBtn}
                    onPress={() => {
                      const pathName = (() => {
                        const s = item.stage.toLowerCase();
                        if (s === "grower validation") return "/validation";
                        return `/inspection/${s}`;
                      })();
                      router.push({
                        pathname: pathName as any,
                        params: {
                          editingReportId: item.id,
                          grower: JSON.stringify(item.grower),
                          inspectorName: item.audit.inspectorName,
                          gps: item.audit.gps,
                          deviceId: item.audit.deviceId,
                          inspectionId: item.inspectionId || "",
                        },
                      });
                    }}
                  >
                    <MaterialIcons name="edit" size={14} color="#0B6B3A" />
                    <ThemedText style={styles.editBtnText} numberOfLines={1} ellipsizeMode="tail">
                      Edit
                    </ThemedText>
                  </Pressable>
                )}

                {!isSynced && (
                  <Pressable
                    style={styles.deleteBtn}
                    onPress={() => promptDelete(item.id, item.grower.name)}
                  >
                    <MaterialIcons name="delete-outline" size={15} color="#DC2626" />
                  </Pressable>
                )}

                {!isSynced && (
                  <Pressable
                    style={[
                      styles.syncBtn,
                      (syncingAll || Boolean(syncingId) || isOffline) && styles.syncBtnDisabled,
                    ]}
                    onPress={() => syncOne(item.id)}
                    disabled={syncingAll || Boolean(syncingId) || isOffline}
                  >
                    <MaterialIcons
                      name={syncingId === item.id ? "sync" : "cloud-upload"}
                      size={14}
                      color={syncingId === item.id || isOffline ? "#9CA3AF" : Colors[theme].primary}
                    />
                    <ThemedText
                      style={[
                        styles.syncBtnText,
                        (syncingId === item.id || isOffline) && { color: "#9CA3AF" },
                      ]}
                      numberOfLines={1}
                      ellipsizeMode="tail"
                    >
                      {syncingId === item.id ? "Syncing…" : "Sync"}
                    </ThemedText>
                  </Pressable>
                )}
              </View>
            </View>
          );
        }}
        ListEmptyComponent={
          <View style={[styles.empty, { borderColor: Colors[theme].border, backgroundColor: Colors[theme].surface }]}>
            <ThemedText type="defaultSemiBold">No saved reports</ThemedText>
            <ThemedText>Submit a Nursery, Field, Curing, or Grower Validation inspection to see it here.</ThemedText>
          </View>
        }
      />
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: { padding: 16, gap: 10 },
  offlineBanner: {
    flexDirection: "row",
    alignItems: "center",
    gap: 6,
    backgroundColor: "#374151",
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 8,
  },
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
  stagePill: {
    flexDirection: "row",
    alignItems: "center",
    gap: 5,
    backgroundColor: "#F0F9F4",
    paddingHorizontal: 9,
    paddingVertical: 4,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "#D1FAE5",
  },
  stageLabel: { fontSize: 12, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold, color: "#0B6B3A" },
  statusBadge: {
    flexDirection: "row",
    alignItems: "center",
    gap: 4,
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 999,
    borderWidth: 1,
  },
  statusDot: { width: 5, height: 5, borderRadius: 3 },
  statusText: { fontSize: 10, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold },
  growerLine: { fontSize: 13, color: "#111827", fontFamily: FontFamily.serif, fontWeight: FontWeight.medium },
  timestamp: { fontSize: 11, color: "#9CA3AF" },
  plainError: {
    flexDirection: "row",
    alignItems: "flex-start",
    gap: 5,
    backgroundColor: "#FEF2F2",
    borderRadius: 6,
    padding: 8,
    borderWidth: 1,
    borderColor: "#FECACA",
  },
  plainErrorText: { flex: 1, fontSize: 11, color: "#DC2626" },
  actions: { flexDirection: "row", gap: 8, marginTop: 2, justifyContent: "flex-end" },
  editBtn: {
    flexDirection: "row",
    alignItems: "center",
    gap: 4,
    paddingHorizontal: 10,
    paddingVertical: 7,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: "#0B6B3A",
    backgroundColor: "#F0F9F4",
  },
  editBtnText: { fontSize: 12, color: "#0B6B3A", fontFamily: FontFamily.serif, fontWeight: FontWeight.medium },
  deleteBtn: {
    width: 36,
    height: 36,
    borderRadius: 8,
    alignItems: "center",
    justifyContent: "center",
    borderWidth: 1,
    borderColor: "#FECACA",
    backgroundColor: "#FEF2F2",
  },
  syncBtn: {
    flexDirection: "row",
    alignItems: "center",
    gap: 5,
    paddingHorizontal: 12,
    paddingVertical: 7,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: "#D1D5DB",
    backgroundColor: "#F9FAFB",
  },
  syncBtnDisabled: { borderColor: "#E5E7EB" },
  syncBtnText: { fontSize: 12, color: "#374151", fontFamily: FontFamily.serif, fontWeight: FontWeight.medium },
  empty: { borderRadius: 16, padding: 20, gap: 6, marginTop: 20, alignItems: "center" },
});
