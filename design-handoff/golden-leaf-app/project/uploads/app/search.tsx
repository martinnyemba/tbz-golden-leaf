import React, { useState } from "react";
import { Alert, View, StyleSheet, TextInput, Pressable, FlatList, Platform } from "react-native";
import { useRouter } from "expo-router";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";

import { ThemedText } from "@/components/themed-text";
import { ThemedView } from "@/components/themed-view";
import { LeafLoader } from "@/components/LeafLoader";
import { Colors } from "@/constants/theme";
import { FontFamily, FontWeight } from "@/constants/typography";
import { useColorScheme } from "@/hooks/use-color-scheme";
import { apiErrorUi, apiFetchJson } from "@/lib/inspection-storage";

type SearchResult = {
  id: string;
  type: string;
  title: string;
  subtitle: string;
  date: string;
};

export default function SearchScreen() {
  const router = useRouter();
  const theme = (useColorScheme() ?? "light") === "dark" ? "dark" : "light";
  
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<SearchResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);

  const handleSearchCommit = () => {
    if (query.trim().length > 1) {
      performSearch(query.trim());
    } else {
      setResults([]);
      setHasSearched(false);
    }
  };

  const performSearch = async (searchQuery: string) => {
    try {
      setLoading(true);
      setHasSearched(true);

      let consolidated: SearchResult[] = [];

      const [growerResp, permitResp] = await Promise.all([
        apiFetchJson(`/api/v1/growers/growers/?search=${encodeURIComponent(searchQuery)}`, { method: "GET" }),
        apiFetchJson(`/api/v1/permits/transport-permits/?search=${encodeURIComponent(searchQuery)}`, { method: "GET" }),
      ]);

      const firstError = !growerResp.ok ? growerResp : (!permitResp.ok ? permitResp : null);
      if (firstError && firstError.status === 0) {
        const ui = apiErrorUi(firstError.status, firstError.body);
        Alert.alert(ui.title, ui.message, [
          { text: "Login", onPress: () => router.push({ pathname: "/portal-login", params: { returnTo: "/search" } } as any) },
          { text: "OK" },
        ]);
        return;
      }

      if (growerResp.ok) {
        const data = JSON.parse(growerResp.body);
        const growers = data.results || (Array.isArray(data) ? data : []);
        growers.slice(0, 10).forEach((g: any) => {
          consolidated.push({
            id: String(g.id),
            type: "Grower Profile",
            title: `${g.first_name || ""} ${g.last_name || ""}`.trim() || g.display_name,
            subtitle: g.tbz_id || g.nrc_number || g.phone_number || "Unknown ID",
            date: g.created_at
          });
        });
      }

      if (permitResp.ok) {
        const data = JSON.parse(permitResp.body);
        const permits = data.results || (Array.isArray(data) ? data : []);
        permits.slice(0, 10).forEach((p: any) => {
          const g = p.grower || {};
          consolidated.push({
            id: String(p.id),
            type: "Transport Permit",
            title: p.license_plate || `Permit ${p.id}`,
            subtitle: `${g.first_name || ""} ${g.last_name || ""} (${p.status || "Unknown"})`,
            date: p.created_at
          });
        });
      }

      if (consolidated.length === 0 && firstError && !firstError.ok) {
        const ui = apiErrorUi(firstError.status, firstError.body);
        Alert.alert(ui.title, ui.message);
      }
      setResults(consolidated);
    } catch (e) {
      setResults([]);
      const msg = e instanceof Error ? e.message : "Search failed.";
      Alert.alert("Search failed", msg);
    } finally {
      setLoading(false);
    }
  };

  const getIconForType = (type: string) => {
    switch (type) {
      case "Grower Profile": return "eco";
      case "Bale Capture": return "dashboard-customize";
      case "Transport Permit": return "local-shipping";
      case "Grower Validation": return "fact-check";
      case "Bale Arbitration": return "gavel";
      default: return "search";
    }
  };

  const navigateToResult = (item: SearchResult) => {
    switch (item.type) {
      case "Grower Profile":
        router.push(`/registration/grower-details?id=${item.id}` as any);
        break;
      case "Transport Permit":
        router.push(`/permit-detail?id=${item.id}` as any);
        break;
      // Other maps can be added as those screens are fleshed out
      default:
        break;
    }
  };

  const renderItem = ({ item }: { item: SearchResult }) => (
    <Pressable 
      onPress={() => navigateToResult(item)}
      style={[styles.resultCard, { backgroundColor: Colors[theme].surface, borderColor: Colors[theme].border }]}
    >
      <View style={[styles.iconBox, { backgroundColor: Colors[theme].primary + '1A' }]}>
        <MaterialIcons name={getIconForType(item.type) as any} size={24} color={Colors[theme].primary} />
      </View>
      <View style={styles.cardContent}>
        <ThemedText style={styles.resultType}>{item.type}</ThemedText>
        <ThemedText style={styles.resultTitle} numberOfLines={1}>{item.title}</ThemedText>
        <ThemedText style={styles.resultSubtitle} numberOfLines={1}>{item.subtitle}</ThemedText>
      </View>
      <MaterialIcons name="chevron-right" size={24} color={Colors[theme].muted} />
    </Pressable>
  );

  return (
    <ThemedView style={styles.container}>
      {/* Custom Rounded Search Header */}
      <View style={[styles.header, { backgroundColor: Colors[theme].primary }]}>
        <View style={styles.headerTop}>
          <Pressable onPress={() => router.back()} style={styles.backButton}>
            <MaterialIcons name="arrow-back" size={24} color="#FFFFFF" />
          </Pressable>
          <ThemedText style={styles.screenTitle} numberOfLines={1} ellipsizeMode="tail">
            Search
          </ThemedText>
        </View>

        <View style={styles.searchRow}>
          <MaterialIcons name="search" size={24} color="#FFFFFF" />
          <TextInput
            style={styles.searchInputCustom}
            placeholder="search registrations, permit..."
            placeholderTextColor="rgba(255, 255, 255, 0.7)"
            value={query}
            onChangeText={(v) => {
              setQuery(v);
              if (v.trim().length === 0) {
                setResults([]);
                setHasSearched(false);
              }
            }}
            autoFocus={true}
            returnKeyType="search"
            onSubmitEditing={handleSearchCommit}
            selectionColor="#FFFFFF"
          />
          {query.length > 0 ? (
            <Pressable onPress={() => setQuery("")} style={styles.actionIcon}>
              <MaterialIcons name="close" size={24} color="#FFFFFF" />
            </Pressable>
          ) : (
            <Pressable onPress={() => {}} style={styles.actionIcon}>
              <MaterialIcons name="tune" size={24} color="#FFFFFF" />
            </Pressable>
          )}
        </View>
        <View style={styles.searchUnderline} />
      </View>

      {/* Results */}
      {loading ? (
        <View style={{ marginTop: 40, alignItems: "center" }}>
          <LeafLoader size={42} />
        </View>
      ) : (
        <FlatList
          data={results}
          keyExtractor={(item) => item.id + item.type}
          contentContainerStyle={styles.listContent}
          renderItem={renderItem}
          ListEmptyComponent={
            hasSearched && query.trim().length > 1 ? (
              <View style={styles.emptyState}>
                <MaterialIcons name="search-off" size={64} color={Colors[theme].muted} />
                <ThemedText style={styles.emptyTitle}>No results found</ThemedText>
                <ThemedText style={styles.emptySubtitle}>
                  We couldn&apos;t find anything matching &quot;{query}&quot;. Try different keywords.
                </ThemedText>
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
  header: {
    paddingTop: Platform.OS === "android" ? 40 : 60,
    paddingBottom: 24,
    paddingHorizontal: 20,
    borderBottomLeftRadius: 32,
    borderBottomRightRadius: 32,
    elevation: 4,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.2,
    shadowRadius: 4,
  },
  headerTop: {
    flexDirection: "row",
    marginBottom: 4,
    alignItems: "center",
    gap: 10,
  },
  backButton: {
    padding: 4,
    marginLeft: -4,
  },
  screenTitle: { color: "#FFFFFF", fontSize: 22, fontFamily: FontFamily.serif, fontWeight: FontWeight.semiBold, flexShrink: 1 },
  searchRow: {
    flexDirection: "row",
    alignItems: "center",
    marginTop: 8,
  },
  searchInputCustom: {
    flex: 1,
    color: "#FFFFFF",
    fontSize: 16,
    marginLeft: 12,
    marginRight: 12,
    fontFamily: FontFamily.serif,
    fontWeight: FontWeight.regular,
    paddingVertical: 8,
  },
  actionIcon: {
    padding: 4,
  },
  searchUnderline: {
    height: 1,
    backgroundColor: "#FFFFFF",
    marginTop: 4,
    opacity: 0.5,
  },
  listContent: {
    padding: 16,
    gap: 12,
  },
  resultCard: {
    flexDirection: "row",
    alignItems: "center",
    padding: 16,
    borderWidth: 1,
    borderRadius: 12,
  },
  iconBox: {
    width: 48,
    height: 48,
    borderRadius: 24,
    justifyContent: "center",
    alignItems: "center",
    marginRight: 16,
  },
  cardContent: {
    flex: 1,
  },
  resultType: {
    fontSize: 12,
    color: "#6B7280",
    textTransform: "uppercase",
    fontWeight: "bold",
    marginBottom: 4,
  },
  resultTitle: {
    fontSize: 16,
    fontWeight: "600",
    marginBottom: 2,
  },
  resultSubtitle: {
    fontSize: 14,
    color: "#6B7280",
  },
  emptyState: {
    alignItems: "center",
    justifyContent: "center",
    marginTop: 80,
    paddingHorizontal: 32,
  },
  emptyTitle: {
    fontSize: 20,
    fontWeight: "bold",
    marginTop: 16,
    marginBottom: 8,
  },
  emptySubtitle: {
    fontSize: 15,
    color: "#6B7280",
    textAlign: "center",
    lineHeight: 22,
  },
});
