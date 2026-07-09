import React, { useState, useCallback, useEffect } from "react";
import { FlatList, View, StyleSheet, TextInput, Pressable } from "react-native";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import { LeafLoader } from "@/components/LeafLoader";
import { ThemedText } from "@/components/themed-text";
import { ThemedView } from "@/components/themed-view";
import { TextField, SelectField, PrimaryButton } from "@/components/ui/form-controls";
import { useColorScheme } from "@/hooks/use-color-scheme";
import { Colors } from "@/constants/theme";
import { FontFamily, FontWeight } from "@/constants/typography";
import { apiFetchJson } from "@/lib/inspection-storage";
import { readCacheItems } from "@/lib/offline-cache";
import { useRouter } from "expo-router";

type PortalGrower = {
  id: string;
  display_name?: string;
  nrc_number?: string;
  tbz_id?: string | null;
  province?: string;
  district?: string;
  phone_number?: string;
};

export default function GrowersListScreen() {
  const theme = (useColorScheme() ?? "light") === "dark" ? "dark" : "light";
  const router = useRouter();

  const [growers, setGrowers] = useState<PortalGrower[]>([]);
  const [loading, setLoading] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [hasMore, setHasMore] = useState(true);
  const [offset, setOffset] = useState(0);

  const [searchQuery, setSearchQuery] = useState("");
  const [appliedSearch, setAppliedSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("");

  const STATUS_OPTIONS = [
    { label: "All Statuses", value: "" },
    { label: "Draft", value: "DRAFT" },
    { label: "Pending", value: "PENDING" },
    { label: "Approved", value: "APPROVED" },
    { label: "Active", value: "ACTIVE" },
    { label: "Rejected", value: "REJECTED" },
    { label: "Suspended", value: "SUSPENDED" }
  ];

  const fetchGrowers = useCallback(async (currentOffset: number, query: string = "", status: string = "") => {
    try {
      const isFirstLoad = currentOffset === 0;
      if (isFirstLoad) {
        setLoading(true);
      } else {
        setLoadingMore(true);
      }

      let endpoint = `/api/v1/growers/growers/?limit=20&offset=${currentOffset}`;
      if (query.trim()) endpoint += `&search=${encodeURIComponent(query.trim())}`;
      if (status) endpoint += `&status=${encodeURIComponent(status)}`;

      const resp = await apiFetchJson(endpoint, {
        method: "GET",
      });

      if (resp.ok) {
        const payload = JSON.parse(resp.body);
        const results = payload.results || (Array.isArray(payload) ? payload : []);
        
        setGrowers(prev => isFirstLoad ? results : [...prev, ...results]);
        
        // If we received fewer than 20 items, we've hit the end
        if (results.length < 20) {
          setHasMore(false);
        } else {
          setHasMore(true);
        }
      } else {
        if (isFirstLoad) {
          const cached = await readCacheItems<any>("growers");
          const q = query.trim().toLowerCase();
          const s = status.trim().toUpperCase();
          const filtered = cached.filter((g) => {
            if (s && String(g?.status ?? "").toUpperCase() !== s) return false;
            if (!q) return true;
            const hay = `${g?.display_name ?? ""} ${g?.tbz_id ?? ""} ${g?.nrc_number ?? ""}`.toLowerCase();
            return hay.includes(q);
          });
          if (filtered.length > 0) setGrowers(filtered.slice(0, 200));
          setHasMore(false);
        }
      }
    } catch {
      // Ignore
    } finally {
      setLoading(false);
      setLoadingMore(false);
    }
  }, []);

  useEffect(() => {
    // Initial fetch
    fetchGrowers(0, "", "");
  }, [fetchGrowers]);

  const loadMore = () => {
    if (!loading && !loadingMore && hasMore) {
      const nextOffset = offset + 20;
      setOffset(nextOffset);
      fetchGrowers(nextOffset, appliedSearch, statusFilter);
    }
  };

  const handleSearch = () => {
    setAppliedSearch(searchQuery);
    setOffset(0);
    fetchGrowers(0, searchQuery, statusFilter);
  };

  const handleStatusChange = (val: string) => {
    const rawValue = STATUS_OPTIONS.find(o => o.label === val)?.value ?? "";
    setStatusFilter(rawValue);
    setOffset(0);
    fetchGrowers(0, appliedSearch, rawValue);
  };

  const currentStatusLabel = STATUS_OPTIONS.find(o => o.value === statusFilter)?.label ?? "All Statuses";

  return (
    <ThemedView style={styles.container}>
      {/* Search Header */}
      <View style={[styles.searchBlock, { backgroundColor: Colors[theme].surface, borderBottomColor: Colors[theme].border }]}>
        <View style={[styles.searchContainer, { borderColor: Colors[theme].border }]}>
          <MaterialIcons name="search" size={20} color={Colors[theme].muted} style={styles.searchIcon} />
          <TextInput
            style={[styles.searchInput, { color: Colors[theme].text }]}
            value={searchQuery}
            onChangeText={setSearchQuery}
            placeholder="Search TBZ ID, NRC, Name..."
            placeholderTextColor={Colors[theme].muted}
            onSubmitEditing={handleSearch}
            returnKeyType="search"
          />
        </View>
        <SelectField 
          label="Filter by Status" 
          value={currentStatusLabel} 
          onChange={handleStatusChange} 
          options={STATUS_OPTIONS.map(o => o.label)} 
        />
      </View>

      {loading && growers.length === 0 ? (
        <ThemedView style={styles.loadingContainer}>
          <LeafLoader size={42} />
          <ThemedText style={{ marginTop: 12 }}>Loading growers...</ThemedText>
        </ThemedView>
      ) : (
        <FlatList
          data={growers}
          keyExtractor={(item, index) => `${item.id}-${index}`}
          contentContainerStyle={styles.listContent}
          renderItem={({ item }) => (
          <Pressable 
            style={[styles.card, { borderColor: Colors[theme].border, backgroundColor: Colors[theme].surface }]}
            onPress={() => router.push({ pathname: "/registration/grower-details", params: { id: item.id } } as any)}
          >
            <View style={styles.cardHeader}>
              <ThemedText type="defaultSemiBold" style={{ flexShrink: 1 }} numberOfLines={1} ellipsizeMode="tail">
                {item.display_name || "Unnamed Grower"}
              </ThemedText>
            </View>
            
            <View style={styles.cardBody}>
              <View style={styles.detailRow}>
                <MaterialIcons name="badge" size={16} color={Colors[theme].muted} style={styles.icon} />
                <ThemedText style={[styles.detailText, { color: Colors[theme].muted, flexShrink: 1 }]} numberOfLines={1} ellipsizeMode="tail">
                  TBZ: {item.tbz_id || "N/A"}
                </ThemedText>
                <ThemedText style={[styles.detailText, { color: Colors[theme].muted, marginLeft: 12, flexShrink: 1 }]} numberOfLines={1} ellipsizeMode="tail">
                  NRC: {item.nrc_number || "N/A"}
                </ThemedText>
              </View>

              <View style={styles.detailRow}>
                <MaterialIcons name="pin-drop" size={16} color={Colors[theme].muted} style={styles.icon} />
                <ThemedText style={[styles.detailText, { color: Colors[theme].muted, flexShrink: 1 }]} numberOfLines={1} ellipsizeMode="tail">
                  {item.province || "Unknown Prov."} / {item.district || "Unknown Dist."}
                </ThemedText>
              </View>

              {item.phone_number ? (
                <View style={styles.detailRow}>
                  <MaterialIcons name="phone" size={16} color={Colors[theme].muted} style={styles.icon} />
                  <ThemedText style={[styles.detailText, { color: Colors[theme].muted, flexShrink: 1 }]} numberOfLines={1} ellipsizeMode="tail">
                    {item.phone_number}
                  </ThemedText>
                </View>
              ) : null}
            </View>
          </Pressable>
        )}
        ListEmptyComponent={
          <View style={styles.emptyContainer}>
            <MaterialIcons name="person-off" size={48} color={Colors[theme].muted} />
            <ThemedText style={{ marginTop: 12, color: Colors[theme].muted }}>No growers found.</ThemedText>
          </View>
        }
        onEndReached={loadMore}
        onEndReachedThreshold={0.5}
        ListFooterComponent={
          loadingMore ? (
            <View style={styles.footerLoader}>
              <LeafLoader size={22} />
              <ThemedText style={{ marginLeft: 8, fontSize: 13, color: Colors[theme].muted }}>Loading more...</ThemedText>
            </View>
          ) : !hasMore && growers.length > 0 ? (
            <View style={styles.footerLoader}>
              <ThemedText style={{ fontSize: 13, color: Colors[theme].muted }}>No more growers.</ThemedText>
            </View>
          ) : null
        }
      />
      )}
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  searchBlock: {
    padding: 16,
    borderBottomWidth: 1,
    gap: 12,
  },
  searchContainer: {
    flexDirection: "row",
    alignItems: "center",
    borderWidth: 1,
    borderRadius: 8,
    paddingHorizontal: 12,
    gap: 8,
    height: 44,
  },
  searchIcon: {
    opacity: 0.8,
  },
  searchInput: {
    flex: 1,
    fontSize: 16,
    paddingVertical: 8,
    fontFamily: FontFamily.serif,
    fontWeight: FontWeight.regular,
  },
  loadingContainer: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
  },
  listContent: {
    padding: 20,
    gap: 12,
  },
  card: {
    borderWidth: 1,
    borderRadius: 14,
    padding: 16,
    overflow: "hidden",
  },
  cardHeader: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    marginBottom: 8,
  },
  cardBody: {
    gap: 6,
  },
  detailRow: {
    flexDirection: "row",
    alignItems: "center",
    flexWrap: "wrap",
  },
  icon: {
    marginRight: 6,
  },
  detailText: {
    fontSize: 13,
  },
  emptyContainer: {
    alignItems: "center",
    justifyContent: "center",
    paddingVertical: 60,
  },
  footerLoader: {
    flexDirection: "row",
    justifyContent: "center",
    alignItems: "center",
    paddingVertical: 16,
  },
});
