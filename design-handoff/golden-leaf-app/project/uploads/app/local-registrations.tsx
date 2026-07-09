import AsyncStorage from "@react-native-async-storage/async-storage";
import { useFocusEffect, useRouter, useLocalSearchParams } from "expo-router";
import { useCallback, useMemo, useState } from "react";
import { Alert, FlatList, Pressable, StyleSheet, View } from "react-native";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";

const DRAFT_KEY = "tbz:growerRegistrationDraft:v1";
const CLEAR_FORM_KEY = "tbz:growerRegistrationClearForm:v1";

import { ThemedText } from "@/components/themed-text";
import { ThemedView } from "@/components/themed-view";
import { PrimaryButton } from "@/components/ui/form-controls";
import { LeafLoader } from "@/components/LeafLoader";
import { ConflictCard, ConflictBadge } from "@/components/ConflictCard";
import { Colors } from "@/constants/theme";
import { useColorScheme } from "@/hooks/use-color-scheme";
import { FontFamily, FontWeight } from "@/constants/typography";
import {
  hasPortalSession,
  listGrowerRegistrations,
  syncGrowerRegistrations,
  deleteGrowerRegistration,
  type LocalGrowerRegistration,
} from "@/lib/inspection-storage";
import { useNetworkStatus } from "@/lib/network-state";

function formatWhen(iso: string) {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString();
}

function fullName(reg: LocalGrowerRegistration) {
  const g = reg.grower;
  return [g.firstName, g.middleName, g.lastName].filter(Boolean).join(" ").trim();
}

const STATUS_COLOR: Record<LocalGrowerRegistration["syncStatus"], string> = {
  pending: "#D97706",
  synced: "#0B6B3A",
  failed: "#DC2626",
  needs_review: "#92400E",
};

export default function LocalRegistrationsScreen() {
  const router = useRouter();
  const { filter } = useLocalSearchParams();
  const theme = (useColorScheme() ?? "light") === "dark" ? "dark" : "light";
  const { isConnected } = useNetworkStatus();
  const isOffline = isConnected === false;

  const [items, setItems] = useState<LocalGrowerRegistration[]>([]);
  const [syncingId, setSyncingId] = useState<string | null>(null);
  const [syncingAll, setSyncingAll] = useState(false);

  const refresh = useCallback(async () => {
    const all = await listGrowerRegistrations();
    setItems(all);
  }, []);

  useFocusEffect(useCallback(() => { refresh(); }, [refresh]));

  const counts = useMemo(() => {
    const pending = items.filter((r) => r.syncStatus === "pending").length;
    const failed = items.filter((r) => r.syncStatus === "failed").length;
    const needsReview = items.filter((r) => r.syncStatus === "needs_review").length;
    const synced = items.filter((r) => r.syncStatus === "synced").length;
    return { pending, failed, needsReview, synced, total: items.length };
  }, [items]);

  const displayedItems = useMemo(() => {
    if (filter === "pending") return items.filter((r) => r.syncStatus === "pending");
    if (filter === "synced") return items.filter((r) => r.syncStatus === "synced");
    if (filter === "failed") return items.filter((r) => r.syncStatus === "failed" || r.syncStatus === "needs_review");
    return items;
  }, [items, filter]);

  const hasPending = counts.pending + counts.failed > 0;

  async function ensureLoggedIn() {
    const ok = await hasPortalSession();
    if (ok) return true;
    router.push({ pathname: "/portal-login", params: { returnTo: "/local-registrations" } } as any);
    return false;
  }

  async function syncAll() {
    if (isOffline) { Alert.alert("Offline", "Connect to the internet to sync registrations."); return; }
    if (!(await ensureLoggedIn())) return;
    setSyncingAll(true);
    try {
      const result = await syncGrowerRegistrations();
      const all = await listGrowerRegistrations();
      setItems(all);
      const failed = all.filter((r) => r.syncStatus === "failed");
      if (result.failedCount === 0) {
        Alert.alert("Sync complete", `${result.syncedCount} registration(s) uploaded successfully.`);
      } else {
        Alert.alert(
          "Partial sync",
          `${result.syncedCount} synced, ${result.failedCount} failed.\n\nCheck each failed card for guidance.`,
        );
        // Show first conflict advice if available
        const firstConflict = failed[0]?.conflictDetail;
        if (firstConflict) {
          setTimeout(() => Alert.alert(firstConflict.summary, firstConflict.advice), 400);
        }
      }
    } catch (e) {
      Alert.alert("Sync failed", e instanceof Error ? e.message : "Sync failed");
    } finally {
      setSyncingAll(false);
    }
  }

  async function syncOne(id: string) {
    if (isOffline) { Alert.alert("Offline", "Connect to the internet to sync this registration."); return; }
    if (!(await ensureLoggedIn())) return;
    setSyncingId(id);
    try {
      await syncGrowerRegistrations([id]);
      const all = await listGrowerRegistrations();
      setItems(all);
      const item = all.find((r) => r.id === id);
      if (item?.syncStatus === "synced") {
        Alert.alert("Success", "Registration uploaded successfully.");
      } else if (item?.conflictDetail) {
        Alert.alert(item.conflictDetail.summary, item.conflictDetail.advice);
      } else if (item?.lastError) {
        Alert.alert("Sync failed", item.lastError);
      }
    } catch (e) {
      Alert.alert("Sync failed", e instanceof Error ? e.message : "Sync failed");
    } finally {
      setSyncingId(null);
    }
  }

  async function promptDelete(id: string, name: string) {
    Alert.alert(
      "Delete Draft",
      `Permanently delete the offline draft for ${name}?`,
      [
        { text: "Cancel", style: "cancel" },
        { text: "Delete", style: "destructive", onPress: async () => { await deleteGrowerRegistration(id); refresh(); } },
      ],
    );
  }

  return (
    <ThemedView style={styles.container}>
      <View style={styles.header}>
        {isOffline && (
          <View style={styles.offlineBanner}>
            <MaterialIcons name="wifi-off" size={14} color="#FFFFFF" />
            <ThemedText style={styles.offlineBannerText}>Offline — sync will resume when connected</ThemedText>
          </View>
        )}

        <ThemedText style={styles.countLine}>
          Total: {counts.total} · Pending: {counts.pending} · Failed: {counts.failed} · Needs review: {counts.needsReview} · Synced: {counts.synced}
        </ThemedText>

        <PrimaryButton
          title={syncingAll ? "Syncing…" : `Sync Pending (${counts.pending + counts.failed})`}
          disabled={!hasPending || syncingAll || isOffline}
          onPress={syncAll}
        />
      </View>

      <FlatList
        data={displayedItems}
        keyExtractor={(item) => item.id}
        contentContainerStyle={styles.list}
        renderItem={({ item }) => {
          const isSynced = item.syncStatus === "synced";
          const needsAttention = item.syncStatus === "failed" || item.syncStatus === "needs_review";
          const borderColor = STATUS_COLOR[item.syncStatus];

          return (
            <View style={[styles.card, { borderLeftColor: borderColor }]}>
              {/* Row top: name + status badge */}
              <View style={styles.rowTop}>
                <ThemedText style={styles.name} numberOfLines={1}>
                  {fullName(item) || "Unnamed grower"}
                </ThemedText>
                <View style={[styles.statusBadge, { backgroundColor: borderColor + "18", borderColor: borderColor + "60" }]}>
                  <View style={[styles.statusDot, { backgroundColor: borderColor }]} />
                  <ThemedText style={[styles.statusText, { color: borderColor }]}>
                    {item.syncStatus.toUpperCase()}
                  </ThemedText>
                </View>
              </View>

              {/* Subline */}
              <ThemedText style={styles.subline}>
                {item.grower.nrcNumber} · {item.grower.province} / {item.grower.district}
              </ThemedText>
              <ThemedText style={styles.timestamp}>{formatWhen(item.createdAt)}</ThemedText>

              {/* Conflict advice card */}
              {needsAttention && item.conflictDetail && (
                <ConflictCard conflict={item.conflictDetail} />
              )}

              {/* Plain error fallback (no conflict detail) */}
              {needsAttention && !item.conflictDetail && item.lastError ? (
                <View style={styles.plainError}>
                  <MaterialIcons name="error-outline" size={13} color="#DC2626" />
                  <ThemedText style={styles.plainErrorText} numberOfLines={3}>{item.lastError}</ThemedText>
                </View>
              ) : null}

              {/* Actions */}
              <View style={styles.actions}>
                {!isSynced && (
                  <Pressable
                    style={styles.editBtn}
                    onPress={() => {
                      Alert.alert("Options", "What would you like to do?", [
                        {
                          text: "Edit / Resubmit",
                          onPress: async () => {
                            const draft = {
                              step: 1,
                              growerType: item.grower.growerType,
                              firstName: item.grower.firstName,
                              middleName: item.grower.middleName,
                              lastName: item.grower.lastName,
                              nrcNumber: item.grower.nrcNumber,
                              sex: item.grower.sex,
                              dateOfBirth: item.grower.dateOfBirth,
                              category: item.grower.category || "Small Scale",
                              phoneNumber: item.grower.phoneNumber,
                              email: item.grower.email,
                              address: item.grower.address,
                              townOrVillage: item.grower.townOrVillage,
                              province: item.grower.province,
                              district: item.grower.district,
                              gpsLatitude: item.grower.gpsLatitude,
                              gpsLongitude: item.grower.gpsLongitude,
                              profilePhotoUri: item.grower.profilePhotoUri,
                              idFrontUri: item.grower.idFrontUri,
                              idBackUri: item.grower.idBackUri,
                              auditDateTime: new Date().toLocaleString("en-GB", {
                                day: "2-digit", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit",
                              }),
                              editingId: item.id,
                              crop: item.crop,
                            };
                            await AsyncStorage.setItem(DRAFT_KEY, JSON.stringify(draft));
                            await AsyncStorage.removeItem(CLEAR_FORM_KEY);
                            router.push("/registration/new");
                          },
                        },
                        { text: "Cancel", style: "cancel" },
                      ]);
                    }}
                  >
                    <MaterialIcons name="edit" size={15} color="#0B6B3A" />
                    <ThemedText style={styles.editBtnText} numberOfLines={1} ellipsizeMode="tail">
                      Edit
                    </ThemedText>
                  </Pressable>
                )}

                {!isSynced && (
                  <Pressable
                    style={styles.deleteBtn}
                    onPress={() => promptDelete(item.id, fullName(item) || "Unnamed grower")}
                  >
                    <MaterialIcons name="delete-outline" size={15} color="#DC2626" />
                  </Pressable>
                )}

                {!isSynced && (
                  <Pressable
                    style={[styles.syncBtn, (syncingId === item.id || syncingAll || isOffline) && styles.syncBtnDisabled]}
                    onPress={() => syncOne(item.id)}
                    disabled={syncingId === item.id || syncingAll || isOffline}
                  >
                    {syncingId === item.id ? (
                      <LeafLoader size={16} />
                    ) : (
                      <MaterialIcons name="cloud-upload" size={15} color={isOffline ? "#9CA3AF" : Colors[theme].primary} />
                    )}
                    <ThemedText
                      style={[styles.syncBtnText, isOffline && { color: "#9CA3AF" }]}
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
            <ThemedText type="defaultSemiBold">No saved registrations</ThemedText>
            <ThemedText>Submit a grower registration to see it listed here.</ThemedText>
          </View>
        }
      />
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: { padding: 20, gap: 10 },
  headerRow: { flexDirection: "row", justifyContent: "space-between", alignItems: "center" },
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
  name: { fontSize: 14, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold, color: "#111827", flex: 1 },
  statusBadge: { flexDirection: "row", alignItems: "center", gap: 4, paddingHorizontal: 8, paddingVertical: 3, borderRadius: 999, borderWidth: 1 },
  statusDot: { width: 5, height: 5, borderRadius: 3 },
  statusText: { fontSize: 10, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold },
  subline: { fontSize: 12, color: "#6B7280" },
  timestamp: { fontSize: 11, color: "#9CA3AF" },
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
