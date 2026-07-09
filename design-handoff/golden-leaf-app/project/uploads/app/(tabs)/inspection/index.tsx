import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import { Link, useFocusEffect, useRouter } from "expo-router";
import { useCallback, useMemo, useState } from "react";
import { Alert, Platform, Pressable, ScrollView, StyleSheet, View } from "react-native";

import { ThemedText } from "@/components/themed-text";
import { ThemedView } from "@/components/themed-view";
import { PrimaryButton } from "@/components/ui/form-controls";
import { Colors } from "@/constants/theme";
import { FontFamily, FontWeight } from "@/constants/typography";
import { useColorScheme } from "@/hooks/use-color-scheme";
import {
  listInspectionReports,
  syncPendingReports,
  listScheduledInspections,
  syncScheduledInspections,
  extractErrorMessage,
  apiFetchJson,
} from "@/lib/inspection-storage";

function normalizeBaseUrl(raw: string) {
  return raw.trim().replace(/\/+$/, "");
}

function sanitizePortalBaseUrl(raw: string) {
  const value = normalizeBaseUrl(raw);
  if (!value) return "";
  try {
    const url = new URL(value);
    return normalizeBaseUrl(`${url.protocol}//${url.host}`);
  } catch {
    try {
      const needsHttp =
        value.startsWith("localhost") ||
        value.startsWith("127.0.0.1") ||
        /^\d{1,3}(\.\d{1,3}){3}(:\d+)?$/.test(value);
      const url = new URL(`${needsHttp ? "http" : "https"}://${value}`);
      return normalizeBaseUrl(`${url.protocol}//${url.host}`);
    } catch {
      return value;
    }
  }
}

function runtimePortalBaseUrl(raw: string) {
  const baseUrl = sanitizePortalBaseUrl(raw);
  if (!baseUrl) return "";
  try {
    const url = new URL(baseUrl);
    const host = url.hostname;
    if (Platform.OS === "android" && (host === "localhost" || host === "127.0.0.1")) {
      url.hostname = "10.0.2.2";
      return normalizeBaseUrl(url.toString());
    }
    return normalizeBaseUrl(url.toString());
  } catch {
    return baseUrl;
  }
}

export default function InspectionHomeScreen() {
  const router = useRouter();
  const theme = (useColorScheme() ?? "light") === "dark" ? "dark" : "light";

  const [localStats, setLocalStats] = useState({ pending: 0, synced: 0, pendingReports: 0, pendingSchedules: 0 });
  const [kpi, setKpi] = useState({ scheduled: 0, in_progress: 0, completed: 0, high_risk: 0 });

  const refreshLocalStats = useCallback(async () => {
    const allReports = await listInspectionReports();
    const pendingReports = allReports.filter((r) => r.syncStatus === "pending" || r.syncStatus === "failed").length;
    const syncedReports = allReports.filter((r) => r.syncStatus === "synced").length;

    const allSchedules = await listScheduledInspections();
    const pendingSchedules = allSchedules.filter((r) => r.syncStatus === "pending" || r.syncStatus === "failed").length;
    const syncedSchedules = allSchedules.filter((r) => r.syncStatus === "synced").length;

    setLocalStats({
      pending: pendingReports + pendingSchedules,
      synced: syncedReports + syncedSchedules,
      pendingReports,
      pendingSchedules
    });
  }, []);

  useFocusEffect(
    useCallback(() => {
      refreshLocalStats();
    }, [refreshLocalStats]),
  );

  const refreshPortalKpi = useCallback(async () => {
    try {
      const [schResp, inPResp, comResp, valResp] = await Promise.all([
        apiFetchJson("/api/v1/inspectorate/inspections/?limit=1&status=SCHEDULED", { method: "GET" }),
        apiFetchJson("/api/v1/inspectorate/inspections/?limit=1&status=IN_PROGRESS", { method: "GET" }),
        apiFetchJson("/api/v1/inspectorate/inspections/?limit=1&status=COMPLETED", { method: "GET" }),
        apiFetchJson("/api/v1/inspectorate/validations/?limit=100", { method: "GET" }),
      ]);

      const schData = schResp.ok ? JSON.parse(schResp.body) : {};
      const inPData = inPResp.ok ? JSON.parse(inPResp.body) : {};
      const comData = comResp.ok ? JSON.parse(comResp.body) : {};
      const valData = valResp.ok ? JSON.parse(valResp.body) : {};

      const scheduled = schData?.count ?? 0;
      const in_progress = inPData?.count ?? 0;
      const completed = comData?.count ?? 0;

      const vals = Array.isArray(valData) ? valData : (valData?.results ?? []);
      const high_risk = vals.filter((v: any) => (Number(v.risk_score) || 0) >= 70).length;

      setKpi({ scheduled, in_progress, completed, high_risk });
    } catch (e) {
      console.log("Failed to fetch KPIs:", e);
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      refreshPortalKpi();
    }, [refreshPortalKpi]),
  );

  const typePills = useMemo(() => {
    return [
      { key: "", label: "All Types" },
      { key: "GROWER_VALIDATION", label: "Grower Validation" },
      { key: "NURSERY_INSPECTION", label: "Nursery" },
      { key: "FIELD_INSPECTION", label: "Field" },
      { key: "CURING_INSPECTION", label: "Curing" },
    ];
  }, []);

  return (
    <ThemedView style={styles.container}>
      <ScrollView style={styles.scroll} contentContainerStyle={styles.content}>
        <View style={styles.topRow}>
          <Pressable
            style={({ pressed }) => [styles.primaryPill, pressed && { opacity: 0.9 }]}
            onPress={() => router.push("/inspection/schedule")}
          >
            <MaterialIcons name="add" size={18} color="#FFFFFF" />
            <ThemedText style={styles.primaryPillText}>Schedule</ThemedText>
          </Pressable>
        </View>

        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.pillsRow}>
          {typePills.map((p) => (
            <Pressable
              key={p.key || "ALL"}
              onPress={() =>
                router.push({
                  pathname: "/inspection/portal-list",
                  params: { title: "Inspections", type: p.key || undefined },
                } as any)
              }
              style={[
                styles.pill,
                { backgroundColor: Colors[theme].surface, borderColor: Colors[theme].border },
              ]}
            >
              <ThemedText style={{ fontSize: 13, color: Colors[theme].text }}>{p.label}</ThemedText>
            </Pressable>
          ))}
        </ScrollView>

        <View style={styles.statsGrid}>
          <Pressable
            style={[
              styles.statCard,
              { backgroundColor: "#E8F3EE", borderColor: Colors[theme].border },
            ]}
            onPress={() => router.push({ pathname: "/inspection/portal-list", params: { status: "SCHEDULED", title: "Scheduled" } } as any)}
          >
            <View style={styles.statTop}>
              <View style={styles.statLabelContainer}>
                <ThemedText style={styles.statLabel} lightColor={Colors[theme].muted} darkColor={Colors[theme].muted}>
                  Scheduled
                </ThemedText>
              </View>
              <View style={[styles.statIconWrap, { backgroundColor: "#FFF7E6" }]}>
                <MaterialIcons name="event" size={18} color={Colors[theme].accent} />
              </View>
            </View>
            <ThemedText type="title" style={{ color: "#0B6B3A" }}>
              {kpi.scheduled}
            </ThemedText>
          </Pressable>

          <Pressable
            style={[
              styles.statCard,
              { backgroundColor: Colors[theme].surface, borderColor: Colors[theme].border },
            ]}
            onPress={() => router.push({ pathname: "/inspection/portal-list", params: { status: "IN_PROGRESS", title: "In Progress" } } as any)}
          >
            <View style={styles.statTop}>
              <View style={styles.statLabelContainer}>
                <ThemedText style={styles.statLabel} lightColor={Colors[theme].muted} darkColor={Colors[theme].muted}>
                  In Progress
                </ThemedText>
              </View>
              <View style={[styles.statIconWrap, { backgroundColor: "#FFF7E6" }]}>
                <MaterialIcons name="autorenew" size={18} color={Colors[theme].accent} />
              </View>
            </View>
            <ThemedText type="title" style={{ color: "#F59E0B" }}>
              {kpi.in_progress}
            </ThemedText>
          </Pressable>

          <Pressable
            style={[
              styles.statCard,
              { backgroundColor: Colors[theme].surface, borderColor: Colors[theme].border },
            ]}
            onPress={() => router.push({ pathname: "/inspection/portal-list", params: { status: "COMPLETED", title: "Completed" } } as any)}
          >
            <View style={styles.statTop}>
              <View style={styles.statLabelContainer}>
                <ThemedText style={styles.statLabel} lightColor={Colors[theme].muted} darkColor={Colors[theme].muted}>
                  Completed
                </ThemedText>
              </View>
              <View style={[styles.statIconWrap, { backgroundColor: "#FFF7E6" }]}>
                <MaterialIcons name="check-circle-outline" size={18} color={Colors[theme].accent} />
              </View>
            </View>
            <ThemedText type="title" style={{ color: "#0B6B3A" }}>
              {kpi.completed}
            </ThemedText>
          </Pressable>

          <Pressable
            style={[
              styles.statCard,
              { backgroundColor: Colors[theme].surface, borderColor: Colors[theme].border },
            ]}
            onPress={() => router.push({ pathname: "/inspection/high-risk", params: { title: "High Risk Growers" } } as any)}
          >
            <View style={styles.statTop}>
              <View style={styles.statLabelContainer}>
                <ThemedText style={styles.statLabel} lightColor={Colors[theme].muted} darkColor={Colors[theme].muted}>
                  High Risk Growers
                </ThemedText>
              </View>
              <View style={[styles.statIconWrap, { backgroundColor: "#FFF7E6" }]}>
                <MaterialIcons name="warning-amber" size={18} color={Colors[theme].accent} />
              </View>
            </View>
            <ThemedText type="title" style={{ color: "#DC2626" }}>
              {kpi.high_risk}
            </ThemedText>
          </Pressable>

          <Pressable
            style={[
              styles.statCard,
              { backgroundColor: Colors[theme].surface, borderColor: Colors[theme].border },
            ]}
            onPress={() => router.push("/inspection/schedules-local" as any)}
          >
            <View style={styles.statTop}>
              <View style={styles.statLabelContainer}>
                <ThemedText style={styles.statLabel} lightColor={Colors[theme].muted} darkColor={Colors[theme].muted}>
                  Scheduled Pending Sync
                </ThemedText>
              </View>
              <View style={[styles.statIconWrap, { backgroundColor: "#FFF7E6" }]}>
                <MaterialIcons name="schedule-send" size={18} color={Colors[theme].accent} />
              </View>
            </View>
            <ThemedText type="title" style={{ color: "#F59E0B" }}>
              {localStats.pendingSchedules}
            </ThemedText>
          </Pressable>

          <Pressable
            style={[
              styles.statCard,
              { backgroundColor: Colors[theme].surface, borderColor: Colors[theme].border },
            ]}
            onPress={() => router.push("/inspection/reports" as any)}
          >
            <View style={styles.statTop}>
              <View style={styles.statLabelContainer}>
                <ThemedText style={styles.statLabel} lightColor={Colors[theme].muted} darkColor={Colors[theme].muted}>
                  Conducted Pending Sync
                </ThemedText>
              </View>
              <View style={[styles.statIconWrap, { backgroundColor: "#FFF7E6" }]}>
                <MaterialIcons name="offline-pin" size={18} color={Colors[theme].accent} />
              </View>
            </View>
            <ThemedText type="title" style={{ color: "#F59E0B" }}>
              {localStats.pendingReports}
            </ThemedText>
          </Pressable>
        </View>

        <View style={[styles.syncCard, { backgroundColor: Colors[theme].surface, borderColor: Colors[theme].border }]}>
          <View style={styles.syncTop}>
            <ThemedText type="defaultSemiBold">Offline Sync</ThemedText>
            <Link href="/inspection/reports" asChild>
              <Pressable>
                <ThemedText type="link">View saved reports</ThemedText>
              </Pressable>
            </Link>
          </View>
          <ThemedText style={styles.syncText} lightColor={Colors[theme].muted} darkColor={Colors[theme].muted}>
            Pending: {localStats.pending} • Synced: {localStats.synced}
          </ThemedText>
          <PrimaryButton
            title="Sync now"
            onPress={async () => {
              const resReports = await syncPendingReports();
              const resSchedules = await syncScheduledInspections();
              await refreshLocalStats();
              if (resReports.requiresLogin || resSchedules.requiresLogin) {
                Alert.alert("Portal login required", "Login to sync saved inspections to the web portal.", [
                  {
                    text: "Login",
                    onPress: () =>
                      router.push({ pathname: "/portal-login", params: { returnTo: "/inspection" } } as any),
                  },
                  { text: "OK" },
                ]);
                return;
              }

              const totalSynced = resReports.syncedCount + resSchedules.syncedCount;
              const totalFailed = resReports.failedCount + resSchedules.failedCount;

              Alert.alert(
                "Sync complete",
                `Synced ${totalSynced} record(s).\nFailed ${totalFailed} record(s).`
              );
            }}
            disabled={localStats.pending === 0}
          />
        </View>
      </ScrollView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#FFFFFF",
  },
  scroll: {
    flex: 1,
  },
  content: {
    padding: 20,
    gap: 14,
    paddingBottom: 40,
  },
  topRow: { flexDirection: "row", alignItems: "center", justifyContent: "flex-end", gap: 12, marginBottom: 4 },
  primaryPill: { backgroundColor: "#0B6B3A", paddingHorizontal: 14, paddingVertical: 10, borderRadius: 999, flexDirection: "row", alignItems: "center", gap: 8 },
  primaryPillText: { color: "#FFFFFF", fontSize: 13, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold },
  breadcrumbRow: { flexDirection: "row", alignItems: "center" },
  breadcrumbMuted: { fontSize: 12 },
  statsGrid: { flexDirection: "row", flexWrap: "wrap", justifyContent: "space-between" },
  pillsRow: { gap: 8, paddingBottom: 4 },
  pill: { borderWidth: 1, borderRadius: 999, paddingHorizontal: 12, paddingVertical: 8 },
  statCard: { width: "48%", borderWidth: 1, borderRadius: 18, padding: 14, marginBottom: 16, gap: 6, overflow: "hidden", backgroundColor: "#FFFFFF" },
  statTop: { flexDirection: "row", justifyContent: "space-between", alignItems: "flex-start", width: "100%" },
  statLabelContainer: { flex: 1, paddingRight: 4, justifyContent: "flex-start" },
  statLabel: { fontSize: 13, flexWrap: "wrap", lineHeight: 18, fontFamily: FontFamily.serif, fontWeight: FontWeight.regular },
  statIconWrap: { width: 36, height: 36, borderRadius: 12, alignItems: "center", justifyContent: "center", flexShrink: 0 },
  syncCard: {
    borderWidth: 1,
    borderRadius: 18,
    padding: 14,
    gap: 10,
  },
  syncTop: { flexDirection: "row", justifyContent: "space-between", alignItems: "center" },
  syncText: { fontSize: 12, fontFamily: FontFamily.serif, fontWeight: FontWeight.regular },
});
