import React, { useCallback, useState } from "react";
import {
  Alert,
  Pressable,
  RefreshControl,
  ScrollView,
  StyleSheet,
  View,
} from "react-native";
import { useRouter } from "expo-router";
import { useFocusEffect } from "expo-router";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";

import { ThemedText } from "@/components/themed-text";
import { ThemedView } from "@/components/themed-view";
import { PrimaryButton } from "@/components/ui/form-controls";
import {
  getPendingSaleCaptures,
  retrySaleCapture,
  deleteSyncedSaleCaptures,
  syncPendingSaleCaptures,
  discardPendingSaleCapture,
  type PendingSaleCapture,
} from "@/lib/inspection-storage";
import { useNetworkStatus } from "@/lib/network-state";
import { CONFLICT_UI, type ConflictCategory } from "@/lib/sync-conflict";
import { FontFamily, FontWeight } from "@/constants/typography";

const SYNC_STATUS_COLOR = {
  pending: "#D97706",
  synced: "#0B6B3A",
  failed: "#DC2626",
  needs_review: "#92400E",
} as const;

const SYNC_STATUS_LABEL = {
  pending: "Pending",
  synced: "Synced",
  failed: "Failed",
  needs_review: "Needs Review",
} as const;

function ConflictBadge({ category }: { category: ConflictCategory }) {
  const ui = CONFLICT_UI[category];
  return (
    <View style={[styles.conflictBadge, { backgroundColor: ui.bg, borderColor: ui.border }]}>
      <MaterialIcons name={ui.icon as any} size={11} color={ui.text} />
      <ThemedText style={[styles.conflictBadgeText, { color: ui.text }]}>{ui.label}</ThemedText>
    </View>
  );
}

function SaleCard({
  sale,
  isOffline,
  retryingId,
  onRetry,
  onEdit,
  onDiscard,
}: {
  sale: PendingSaleCapture;
  isOffline: boolean;
  retryingId: string | null;
  onRetry: (id: string) => void;
  onEdit: (id: string) => void;
  onDiscard: (id: string) => void;
}) {
  const conflict = sale.conflictDetail;
  const isRetrying = retryingId === sale.id;
  const needsAttention = sale.syncStatus === "failed" || sale.syncStatus === "needs_review";

  return (
    <View style={styles.card}>
      {/* Header row: sync status + conflict badge + timestamp */}
      <View style={styles.cardHeader}>
        <View style={styles.badgeRow}>
          <View style={[styles.statusBadge, { borderColor: SYNC_STATUS_COLOR[sale.syncStatus] + "60", backgroundColor: SYNC_STATUS_COLOR[sale.syncStatus] + "18" }]}>
            <View style={[styles.statusDot, { backgroundColor: SYNC_STATUS_COLOR[sale.syncStatus] }]} />
            <ThemedText style={[styles.statusText, { color: SYNC_STATUS_COLOR[sale.syncStatus] }]}>
              {SYNC_STATUS_LABEL[sale.syncStatus]}
            </ThemedText>
          </View>
          {conflict && <ConflictBadge category={conflict.category} />}
        </View>
        <ThemedText style={styles.timestamp}>
          {new Date(sale.createdAt).toLocaleDateString()}{" "}
          {new Date(sale.createdAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
        </ThemedText>
      </View>

      {/* Sale details grid */}
      <View style={styles.detailGrid}>
        <DetailItem label="Bales" value={String(sale.meta.baleCount)} />
        {sale.meta.growerName ? <DetailItem label="Grower" value={sale.meta.growerName} /> : null}
        {sale.meta.salesfloorName ? <DetailItem label="Sales Floor" value={sale.meta.salesfloorName} /> : null}
        {sale.meta.permitNumber ? <DetailItem label="Permit" value={sale.meta.permitNumber} /> : null}
        {sale.meta.saleDate ? <DetailItem label="Sale Date" value={sale.meta.saleDate} /> : null}
      </View>

      {/* Conflict advice box */}
      {conflict && needsAttention && (
        <View style={[styles.adviceBox, { backgroundColor: CONFLICT_UI[conflict.category].bg, borderColor: CONFLICT_UI[conflict.category].border }]}>
          <ThemedText style={[styles.adviceSummary, { color: CONFLICT_UI[conflict.category].text }]}>
            {conflict.summary}
          </ThemedText>
          <ThemedText style={[styles.adviceText, { color: CONFLICT_UI[conflict.category].text }]}>
            {conflict.advice}
          </ThemedText>
          {conflict.rawError && conflict.rawError !== conflict.summary && (
            <ThemedText style={styles.rawError} numberOfLines={3}>
              Server: {conflict.rawError}
            </ThemedText>
          )}
        </View>
      )}

      {/* Last error (no conflict detail) */}
      {!conflict && sale.lastError && needsAttention && (
        <View style={styles.genericErrorBox}>
          <MaterialIcons name="error-outline" size={14} color="#DC2626" />
          <ThemedText style={styles.genericErrorText} numberOfLines={3}>
            {sale.lastError}
          </ThemedText>
        </View>
      )}

      {sale.lastAttemptAt ? (
        <ThemedText style={styles.attemptText}>
          Last attempt: {new Date(sale.lastAttemptAt).toLocaleTimeString()}
        </ThemedText>
      ) : null}

      {/* Action row */}
      {sale.syncStatus !== "synced" && (
        <View style={styles.actionRow}>
          {conflict?.canEdit && (
            <Pressable style={styles.editBtn} onPress={() => onEdit(sale.id)}>
              <MaterialIcons name="edit" size={15} color="#0B6B3A" />
              <ThemedText style={styles.editBtnText} numberOfLines={1} ellipsizeMode="tail">
                Edit & Retry
              </ThemedText>
            </Pressable>
          )}

          {sale.syncStatus !== "needs_review" && (!conflict || conflict.isRetryable || !conflict.isIrresolvable) && !conflict?.canEdit && (
            <Pressable
              style={[styles.retryBtn, (isRetrying || isOffline) && styles.btnDisabled]}
              onPress={() => onRetry(sale.id)}
              disabled={isRetrying || isOffline}
            >
              <MaterialIcons name="refresh" size={15} color={isRetrying || isOffline ? "#9CA3AF" : "#0B6B3A"} />
              <ThemedText
                style={[styles.retryBtnText, (isRetrying || isOffline) && { color: "#9CA3AF" }]}
                numberOfLines={1}
                ellipsizeMode="tail"
              >
                {isRetrying ? "Retrying…" : "Retry"}
              </ThemedText>
            </Pressable>
          )}

          <Pressable style={styles.discardBtn} onPress={() => onDiscard(sale.id)}>
            <MaterialIcons name="delete-outline" size={15} color="#DC2626" />
            <ThemedText style={styles.discardBtnText} numberOfLines={1} ellipsizeMode="tail">
              Discard
            </ThemedText>
          </Pressable>
        </View>
      )}
    </View>
  );
}

function DetailItem({ label, value }: { label: string; value: string }) {
  return (
    <View style={styles.detailItem}>
      <ThemedText style={styles.detailLabel}>{label}</ThemedText>
      <ThemedText style={styles.detailValue} numberOfLines={1}>{value}</ThemedText>
    </View>
  );
}

export default function PendingSalesScreen() {
  const router = useRouter();
  const { isConnected } = useNetworkStatus();
  const isOffline = isConnected === false;

  const [sales, setSales] = useState<PendingSaleCapture[]>([]);
  const [isSyncing, setIsSyncing] = useState(false);
  const [retryingId, setRetryingId] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);

  const load = useCallback(async () => {
    const all = await getPendingSaleCaptures();
    setSales(all.slice().reverse());
  }, []);

  useFocusEffect(useCallback(() => { void load(); }, [load]));

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await load();
    setRefreshing(false);
  }, [load]);

  const handleSyncAll = async () => {
    if (isOffline) {
      Alert.alert("Offline", "Connect to the internet to sync pending sales.");
      return;
    }
    setIsSyncing(true);
    try {
      const { syncedCount, failedCount } = await syncPendingSaleCaptures();
      await deleteSyncedSaleCaptures();
      await load();
      if (failedCount === 0) {
        Alert.alert("Sync Complete", `${syncedCount} sale batch${syncedCount !== 1 ? "es" : ""} submitted successfully.`);
      } else {
        Alert.alert(
          "Partial Sync",
          `${syncedCount} submitted, ${failedCount} failed.\n\nReview each failed record below for guidance on how to resolve the issue.`,
        );
      }
    } catch {
      Alert.alert("Sync Error", "An unexpected error occurred. Please try again.");
    } finally {
      setIsSyncing(false);
    }
  };

  const handleRetry = async (id: string) => {
    if (isOffline) { Alert.alert("Offline", "Connect to the internet to retry this sale."); return; }
    setRetryingId(id);
    try {
      const { success, conflictDetail } = await retrySaleCapture(id);
      await load();
      if (success) {
        await deleteSyncedSaleCaptures();
        await load();
        Alert.alert("Success", "Sale batch submitted successfully.");
      } else if (conflictDetail) {
        Alert.alert(conflictDetail.summary, conflictDetail.advice);
      } else {
        Alert.alert("Failed", "Submission failed. Check the error details on the card.");
      }
    } finally {
      setRetryingId(null);
    }
  };

  const handleEdit = (id: string) => {
    router.push({ pathname: "/edit-pending-sale", params: { id } } as any);
  };

  const handleDiscard = (id: string) => {
    Alert.alert(
      "Discard Record",
      "This offline sale record will be permanently deleted and will not be submitted to the server. Are you sure?",
      [
        { text: "Cancel", style: "cancel" },
        {
          text: "Discard",
          style: "destructive",
          onPress: async () => {
            await discardPendingSaleCapture(id);
            await load();
          },
        },
      ],
    );
  };

  const handleClearSynced = async () => {
    Alert.alert("Clear Synced", "Remove all successfully synced records from this list?", [
      { text: "Cancel", style: "cancel" },
      { text: "Clear", style: "destructive", onPress: async () => { await deleteSyncedSaleCaptures(); await load(); } },
    ]);
  };

  const pendingCount = sales.filter((s) => s.syncStatus !== "synced").length;
  const syncableCount = sales.filter((s) => s.syncStatus === "pending" || s.syncStatus === "failed").length;
  const syncedCount = sales.filter((s) => s.syncStatus === "synced").length;
  const needsReviewCount = sales.filter((s) => s.syncStatus === "needs_review").length;
  const hasIrresolvable = sales.some((s) => s.conflictDetail?.isIrresolvable && s.syncStatus !== "synced");

  return (
    <ThemedView style={styles.container}>
      <View style={styles.header}>
        <Pressable onPress={() => router.back()} style={{ padding: 8, marginLeft: -8, marginRight: 8 }}>
          <MaterialIcons name="arrow-back" size={26} color="#111827" />
        </Pressable>
        <View style={{ flex: 1 }}>
          <ThemedText type="subtitle" style={{ fontSize: 16 }}>Offline Sales Queue</ThemedText>
          <ThemedText style={{ fontSize: 12, color: "#6B7280" }}>
            {pendingCount} pending · {needsReviewCount} need review · {syncedCount} synced
          </ThemedText>
        </View>
      </View>

      {isOffline && (
        <View style={styles.offlineBanner}>
          <MaterialIcons name="wifi-off" size={15} color="#FFFFFF" />
          <ThemedText style={styles.offlineBannerText}>
            Offline — connect to submit pending sales
          </ThemedText>
        </View>
      )}

      {hasIrresolvable && (
        <View style={styles.irresolvableBanner}>
          <MaterialIcons name="warning" size={15} color="#92400E" />
          <ThemedText style={styles.irresolvableBannerText}>
            Some records cannot be auto-fixed. Review each card's advice and discard if unresolvable.
          </ThemedText>
        </View>
      )}

      {syncableCount > 0 && (
        <View style={styles.syncActionRow}>
          <PrimaryButton
            title={isSyncing ? "Syncing…" : `Sync All (${syncableCount})`}
            onPress={handleSyncAll}
            disabled={isSyncing || isOffline}
          />
        </View>
      )}

      <ScrollView
        contentContainerStyle={styles.content}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
      >
        {sales.length === 0 && (
          <View style={styles.emptyState}>
            <MaterialIcons name="check-circle" size={48} color="#0B6B3A" />
            <ThemedText style={styles.emptyTitle}>All caught up</ThemedText>
            <ThemedText style={styles.emptySubtitle}>No pending offline sales.</ThemedText>
          </View>
        )}

        {sales.map((sale) => (
          <SaleCard
            key={sale.id}
            sale={sale}
            isOffline={isOffline}
            retryingId={retryingId}
            onRetry={handleRetry}
            onEdit={handleEdit}
            onDiscard={handleDiscard}
          />
        ))}

        {syncedCount > 0 && (
          <Pressable style={styles.clearBtn} onPress={handleClearSynced}>
            <MaterialIcons name="delete-sweep" size={16} color="#6B7280" />
            <ThemedText style={styles.clearBtnText}>
              Clear {syncedCount} synced record{syncedCount !== 1 ? "s" : ""}
            </ThemedText>
          </Pressable>
        )}
      </ScrollView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#F9FAFB" },
  header: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 20,
    paddingTop: 60,
    paddingBottom: 16,
    backgroundColor: "#FFFFFF",
    borderBottomWidth: 1,
    borderBottomColor: "#E5E7EB",
  },
  offlineBanner: { flexDirection: "row", alignItems: "center", gap: 8, backgroundColor: "#374151", paddingHorizontal: 16, paddingVertical: 8 },
  offlineBannerText: { color: "#FFFFFF", fontSize: 12, flex: 1 },
  irresolvableBanner: { flexDirection: "row", alignItems: "center", gap: 8, backgroundColor: "#FEF3C7", borderBottomWidth: 1, borderBottomColor: "#FDE68A", paddingHorizontal: 16, paddingVertical: 8 },
  irresolvableBannerText: { color: "#92400E", fontSize: 12, flex: 1 },
  syncActionRow: { padding: 16, paddingBottom: 0 },
  content: { padding: 16, paddingBottom: 60, gap: 12 },
  emptyState: { alignItems: "center", paddingVertical: 64, gap: 12 },
  emptyTitle: { fontSize: 18, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold, color: "#111827" },
  emptySubtitle: { fontSize: 14, color: "#6B7280" },

  card: { backgroundColor: "#FFFFFF", borderRadius: 12, padding: 16, borderWidth: 1, borderColor: "#E5E7EB", gap: 10, overflow: "hidden" },
  cardHeader: { flexDirection: "row", alignItems: "center", justifyContent: "space-between", gap: 8 },
  badgeRow: { flexDirection: "row", alignItems: "center", gap: 6, flexShrink: 1, flexWrap: "wrap" },

  statusBadge: { flexDirection: "row", alignItems: "center", gap: 5, paddingHorizontal: 9, paddingVertical: 3, borderRadius: 999, borderWidth: 1 },
  statusDot: { width: 6, height: 6, borderRadius: 3 },
  statusText: { fontSize: 11, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold },

  conflictBadge: { flexDirection: "row", alignItems: "center", gap: 4, paddingHorizontal: 8, paddingVertical: 3, borderRadius: 999, borderWidth: 1 },
  conflictBadgeText: { fontSize: 10, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold },

  timestamp: { fontSize: 11, color: "#9CA3AF", flexShrink: 0 },

  detailGrid: { flexDirection: "row", flexWrap: "wrap", gap: 8 },
  detailItem: { minWidth: "44%", flex: 1 },
  detailLabel: { fontSize: 10, color: "#9CA3AF", marginBottom: 1 },
  detailValue: { fontSize: 13, fontFamily: FontFamily.serif, fontWeight: FontWeight.medium, color: "#111827" },

  adviceBox: { borderRadius: 8, padding: 12, borderWidth: 1, gap: 4 },
  adviceSummary: { fontSize: 12, fontFamily: FontFamily.serif, fontWeight: FontWeight.bold },
  adviceText: { fontSize: 12, lineHeight: 18 },
  rawError: { fontSize: 10, color: "#6B7280", marginTop: 4, fontFamily: FontFamily.serif },

  genericErrorBox: { flexDirection: "row", alignItems: "flex-start", gap: 6, backgroundColor: "#FEF2F2", borderRadius: 8, padding: 10, borderWidth: 1, borderColor: "#FECACA" },
  genericErrorText: { flex: 1, fontSize: 12, color: "#DC2626" },

  attemptText: { fontSize: 10, color: "#9CA3AF" },

  actionRow: { flexDirection: "row", gap: 8, flexWrap: "wrap" },
  editBtn: { flex: 1, flexDirection: "row", alignItems: "center", justifyContent: "center", gap: 5, paddingVertical: 9, borderRadius: 8, borderWidth: 1, borderColor: "#0B6B3A", backgroundColor: "#F0F9F4", minWidth: 100 },
  editBtnText: { fontSize: 13, color: "#0B6B3A", fontFamily: FontFamily.serif, fontWeight: FontWeight.medium },
  retryBtn: { flex: 1, flexDirection: "row", alignItems: "center", justifyContent: "center", gap: 5, paddingVertical: 9, borderRadius: 8, borderWidth: 1, borderColor: "#0B6B3A", backgroundColor: "#F0F9F4", minWidth: 80 },
  retryBtnText: { fontSize: 13, color: "#0B6B3A", fontFamily: FontFamily.serif, fontWeight: FontWeight.medium },
  btnDisabled: { borderColor: "#E5E7EB", backgroundColor: "#F9FAFB" },
  discardBtn: { flexDirection: "row", alignItems: "center", justifyContent: "center", gap: 5, paddingVertical: 9, paddingHorizontal: 14, borderRadius: 8, borderWidth: 1, borderColor: "#FECACA", backgroundColor: "#FEF2F2" },
  discardBtnText: { fontSize: 13, color: "#DC2626", fontFamily: FontFamily.serif, fontWeight: FontWeight.medium },

  clearBtn: { flexDirection: "row", alignItems: "center", justifyContent: "center", gap: 6, paddingVertical: 12, borderRadius: 8, borderWidth: 1, borderColor: "#E5E7EB", backgroundColor: "#FFFFFF" },
  clearBtnText: { fontSize: 13, color: "#6B7280" },
});
