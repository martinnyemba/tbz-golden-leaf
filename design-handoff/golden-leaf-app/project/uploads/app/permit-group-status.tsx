import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import AsyncStorage from "@react-native-async-storage/async-storage";
import * as WebBrowser from "expo-web-browser";
import { Stack, useFocusEffect, useLocalSearchParams, useRouter } from "expo-router";
import React, { useCallback, useMemo, useRef, useState } from "react";
import { Alert, Pressable, ScrollView, StyleSheet, View } from "react-native";

import { LeafLoader } from "@/components/LeafLoader";
import { ThemedText } from "@/components/themed-text";
import { ThemedView } from "@/components/themed-view";
import { PrimaryButton } from "@/components/ui/form-controls";
import { Colors } from "@/constants/theme";
import { FontFamily, FontWeight } from "@/constants/typography";
import { useColorScheme } from "@/hooks/use-color-scheme";
import { apiErrorUi, apiFetchJson, sanitizePortalBaseUrl } from "@/lib/inspection-storage";

type ApiListResponse<T> = { results?: T[]; next?: string | null; count?: number } | T[];

type GroupPermitEntry = {
  id: string;
};

type GroupPermit = {
  id: string;
  group_permit_number?: string | null;
  license_plate: string;
  destination_salesfloor: string;
  total_bales: number;
  total_weight_kg: string | number;
  status: string;
  valid_to?: string | null;
  entries?: GroupPermitEntry[];
};

function listFromApi<T>(payload: ApiListResponse<T>): { items: T[]; next: string; count?: number } {
  if (Array.isArray(payload)) return { items: payload, next: "" };
  const items = Array.isArray(payload?.results) ? payload.results : [];
  const next = typeof payload?.next === "string" ? payload.next : "";
  const count = typeof payload?.count === "number" ? payload.count : undefined;
  return { items, next, count };
}

function safeJson(raw: string) {
  try {
    return { ok: true as const, value: JSON.parse(raw) };
  } catch (e) {
    const msg = e instanceof Error ? e.message : "Invalid JSON";
    return { ok: false as const, error: msg };
  }
}

function formatDate(raw?: string | null) {
  if (!raw) return "—";
  return String(raw).slice(0, 10);
}

function statusBadgeColors(status: string) {
  const s = String(status || "").toUpperCase();
  if (s === "APPROVED") return { bg: "#E8F3EE", text: "#0B6B3A" };
  if (s === "PENDING") return { bg: "#FEF3C7", text: "#D97706" };
  if (s === "DRAFT") return { bg: "#E5E7EB", text: "#374151" };
  return { bg: "#FEE2E2", text: "#B91C1C" };
}

function titleFor(status: string) {
  const s = String(status || "").toUpperCase();
  if (s === "DRAFT") return "Draft Group Permits";
  if (s === "PENDING") return "Pending Review Group Permits";
  if (s === "APPROVED") return "Active Group Permits";
  return "Group Permits";
}

export default function PermitGroupStatusRoute() {
  const router = useRouter();
  const { status } = useLocalSearchParams<{ status?: string }>();
  const theme = (useColorScheme() ?? "light") === "dark" ? "dark" : "light";

  const selectedStatus = useMemo(() => String(status || "").toUpperCase(), [status]);
  const screenTitle = useMemo(() => titleFor(selectedStatus), [selectedStatus]);

  const [canApprovePermits, setCanApprovePermits] = useState(false);
  const [groups, setGroups] = useState<GroupPermit[]>([]);
  const [groupsLoading, setGroupsLoading] = useState(false);
  const [groupsNext, setGroupsNext] = useState<string>("");
  const groupsLoadingRef = useRef(false);
  const groupsNextRef = useRef("");

  const openPortalGroupPermits = useCallback(async () => {
    const savedBase = (await AsyncStorage.getItem("tbz:portalBaseUrl:v1")) ?? "";
    const baseUrl = sanitizePortalBaseUrl(savedBase);
    if (!baseUrl) {
      Alert.alert("Missing portal URL", "Set the portal URL in Portal Login first.");
      return;
    }
    await WebBrowser.openBrowserAsync(`${baseUrl}/permits/group/`);
  }, []);

  const fetchMe = useCallback(async () => {
    const resp = await apiFetchJson("/api/v1/mobile/me/", { method: "GET" });
    if (!resp.ok) return;
    const parsed = safeJson(resp.body);
    if (!parsed.ok) return;
    const perms = parsed.value?.permissions ?? {};
    setCanApprovePermits(Boolean(perms?.can_approve_permits));
  }, []);

  const fetchGroupPermits = useCallback(
    async (opts?: { reset?: boolean }) => {
      if (groupsLoadingRef.current) return;
      groupsLoadingRef.current = true;
      setGroupsLoading(true);
      try {
        const nextPath = opts?.reset ? "" : groupsNextRef.current;
        const basePath = selectedStatus
          ? `/api/v1/permits/group-permits/?status=${encodeURIComponent(selectedStatus)}`
          : "/api/v1/permits/group-permits/";
        const path = nextPath || `${basePath}${basePath.includes("?") ? "&" : "?"}page_size=20`;
        const resp = await apiFetchJson(path, { method: "GET" });
        if (!resp.ok) {
          const ui = apiErrorUi(resp.status, resp.body);
          if (resp.status === 404 || resp.status >= 500 || ui.kind === "network_error") {
            Alert.alert(
              ui.title,
              ui.message,
              [
                { text: "Open Portal", onPress: () => void openPortalGroupPermits() },
                { text: "OK" },
              ],
              { cancelable: true },
            );
          } else {
            Alert.alert(ui.title, ui.message);
          }
          return;
        }
        const parsed = safeJson(resp.body);
        if (!parsed.ok) throw new Error(parsed.error);
        const { items, next } = listFromApi<GroupPermit>(parsed.value);
        const normalizedNext = next
          ? (() => {
              if (next.startsWith("http://") || next.startsWith("https://")) {
                try {
                  const u = new URL(next);
                  return `${u.pathname}${u.search}`;
                } catch {
                  return next;
                }
              }
              return next;
            })()
          : "";
        groupsNextRef.current = normalizedNext;
        setGroupsNext(normalizedNext);
        setGroups((prev) => (opts?.reset ? items : prev.concat(items)));
      } catch (e) {
        const msg = e instanceof Error ? e.message : "Network request failed.";
        Alert.alert("Network error", msg);
      } finally {
        groupsLoadingRef.current = false;
        setGroupsLoading(false);
      }
    },
    [selectedStatus],
  );

  useFocusEffect(
    useCallback(() => {
      setGroups([]);
      setGroupsNext("");
      groupsNextRef.current = "";
      groupsLoadingRef.current = false;
      void fetchMe();
      void fetchGroupPermits({ reset: true });
    }, [fetchGroupPermits, fetchMe, selectedStatus]),
  );

  const openDetail = useCallback(
    (gp: GroupPermit) => {
      router.push({ pathname: "/permit-group-detail" as any, params: { id: gp.id } } as any);
    },
    [router],
  );

  const openEntries = useCallback(
    async (gp: GroupPermit) => {
      router.push({ pathname: "/permit-group-detail" as any, params: { id: gp.id } } as any);
    },
    [router],
  );

  const openApprove = useCallback(
    async (gp: GroupPermit) => {
      router.push({ pathname: "/permit-group-detail" as any, params: { id: gp.id } } as any);
    },
    [router],
  );

  return (
    <ThemedView style={styles.container}>
      <Stack.Screen
        options={{
          headerShown: true,
          title: screenTitle,
          headerStyle: { backgroundColor: Colors[theme].primary },
          headerTitleStyle: { fontFamily: FontFamily.serif, fontWeight: FontWeight.bold },
          headerTintColor: Colors[theme].surface,
        }}
      />

      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        {groupsLoading && groups.length === 0 ? (
          <View style={{ marginTop: 20, alignItems: "center" }}>
            <LeafLoader size={38} />
          </View>
        ) : null}

        {groups.length === 0 && !groupsLoading ? (
          <View style={styles.empty}>
            <MaterialIcons name="folder-open" size={48} color="#9CA3AF" />
            <ThemedText style={{ marginTop: 10 }}>No grouped transport permits found.</ThemedText>
          </View>
        ) : (
          <View style={{ gap: 12, marginTop: 6 }}>
            {groups.map((gp) => {
              const badge = statusBadgeColors(gp.status);
              const permitNo = gp.group_permit_number ? gp.group_permit_number : gp.status === "DRAFT" ? "Draft" : "Pending";
              const growerCount = (gp.entries ?? []).length;
              return (
                <Pressable key={gp.id} onPress={() => openDetail(gp)} style={({ pressed }) => [styles.groupCard, pressed && styles.groupCardPressed]}>
                  <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center", gap: 10 }}>
                    <ThemedText style={styles.groupTitle} numberOfLines={1}>
                      {permitNo}
                    </ThemedText>
                    <View style={[styles.badge, { backgroundColor: badge.bg }]}>
                      <ThemedText style={[styles.badgeText, { color: badge.text }]} numberOfLines={1}>
                        {gp.status}
                      </ThemedText>
                    </View>
                  </View>
                  <ThemedText style={styles.groupMeta}>License Plate: {gp.license_plate}</ThemedText>
                  <ThemedText style={styles.groupMeta}>Growers: {growerCount}</ThemedText>
                  <ThemedText style={styles.groupMeta}>
                    Total: {gp.total_bales} bales • {String(gp.total_weight_kg)} kg
                  </ThemedText>
                  <ThemedText style={styles.groupMeta} numberOfLines={1}>
                    Destination: {gp.destination_salesfloor}
                  </ThemedText>
                  <ThemedText style={styles.groupMeta}>Valid Until: {formatDate(gp.valid_to)}</ThemedText>

                  <View style={styles.inlineActions}>
                    <Pressable
                      onPress={(e) => {
                        e.stopPropagation();
                        openDetail(gp);
                      }}
                      style={({ pressed }) => [styles.cardActionBtn, styles.cardActionBtnInfo, pressed && styles.cardActionBtnPressed]}
                    >
                      <MaterialIcons name="visibility" size={18} color="#2563EB" />
                      <ThemedText style={[styles.cardActionText, styles.cardActionTextInfo]} numberOfLines={1}>
                        View
                      </ThemedText>
                    </Pressable>

                    {gp.status === "DRAFT" ? (
                      <Pressable
                        onPress={(e) => {
                          e.stopPropagation();
                          void openEntries(gp);
                        }}
                        style={({ pressed }) => [styles.cardActionBtn, styles.cardActionBtnNeutral, pressed && styles.cardActionBtnPressed]}
                      >
                        <MaterialIcons name="edit" size={18} color="#6B7280" />
                        <ThemedText style={[styles.cardActionText, styles.cardActionTextNeutral]} numberOfLines={1}>
                          Edit
                        </ThemedText>
                      </Pressable>
                    ) : null}

                    {gp.status === "PENDING" && canApprovePermits ? (
                      <Pressable
                        onPress={(e) => {
                          e.stopPropagation();
                          void openApprove(gp);
                        }}
                        style={({ pressed }) => [styles.cardActionBtn, styles.cardActionBtnSuccess, pressed && styles.cardActionBtnPressed]}
                      >
                        <MaterialIcons name="check-circle" size={18} color="#0B6B3A" />
                        <ThemedText style={[styles.cardActionText, styles.cardActionTextSuccess]} numberOfLines={1}>
                          Review
                        </ThemedText>
                      </Pressable>
                    ) : null}
                  </View>
                </Pressable>
              );
            })}

            {groupsNext ? (
              <View style={{ marginTop: 6 }}>
                <PrimaryButton title={groupsLoading ? "Loading..." : "Load more"} onPress={() => fetchGroupPermits()} disabled={groupsLoading} loading={groupsLoading} />
              </View>
            ) : null}
          </View>
        )}
      </ScrollView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#FFFFFF" },
  content: { padding: 20, paddingBottom: 40, gap: 14 },
  empty: { marginTop: 18, alignItems: "center", paddingVertical: 28, opacity: 0.85 },

  groupCard: { borderWidth: 1, borderColor: "#E5E7EB", borderRadius: 16, padding: 14, backgroundColor: "#FFFFFF", gap: 6 },
  groupCardPressed: { backgroundColor: "#F9FAFB" },
  groupTitle: { fontSize: 14, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold, color: "#111827", flex: 1 },
  groupMeta: { fontSize: 12, color: "#6B7280" },

  badge: { paddingHorizontal: 8, paddingVertical: 4, borderRadius: 999 },
  badgeText: { fontSize: 10, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold },

  inlineActions: { marginTop: 8, flexDirection: "row", flexWrap: "wrap", gap: 8, justifyContent: "flex-start" },
  cardActionBtn: { flexDirection: "row", alignItems: "center", gap: 8, paddingHorizontal: 12, paddingVertical: 10, borderRadius: 12, borderWidth: 1 },
  cardActionBtnPressed: { opacity: 0.85 },
  cardActionBtnInfo: { backgroundColor: "#EFF6FF", borderColor: "#BFDBFE" },
  cardActionBtnNeutral: { backgroundColor: "#F9FAFB", borderColor: "#E5E7EB" },
  cardActionBtnSuccess: { backgroundColor: "#E8F3EE", borderColor: "#B7E0CB" },
  cardActionText: { fontSize: 12, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold, flexShrink: 1 },
  cardActionTextInfo: { color: "#2563EB" },
  cardActionTextNeutral: { color: "#111827" },
  cardActionTextSuccess: { color: "#0B6B3A" },
});
