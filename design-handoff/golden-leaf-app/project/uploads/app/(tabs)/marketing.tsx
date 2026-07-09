import AsyncStorage from "@react-native-async-storage/async-storage";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import { useRouter } from "expo-router";
import { useCallback, useMemo } from "react";
import { Alert, Pressable, ScrollView, StyleSheet, View, useWindowDimensions } from "react-native";

import { ThemedText } from "@/components/themed-text";
import { ThemedView } from "@/components/themed-view";
import { Colors } from "@/constants/theme";
import { FontFamily, FontWeight } from "@/constants/typography";
import { API_BASE_URL_KEY, runtimePortalBaseUrl } from "@/lib/inspection-storage";

type SalesPortalCard = {
  key: string;
  title: string;
  subtitle: string;
  icon: string;
  iconBg: string;
  iconColor: string;
  href: string;
};

const cards: SalesPortalCard[] = [
  {
    key: "capture",
    title: "Capture",
    subtitle: "Sales Data",
    icon: "storefront",
    iconBg: "#E6F0FF",
    iconColor: "#2563EB",
    href: "/sales",
  },
  {
    key: "bookings",
    title: "Bookings",
    subtitle: "Manage",
    icon: "event-note",
    iconBg: "#EFE7FF",
    iconColor: "#6D28D9",
    href: "/pending-sales",
  },
  {
    key: "permit_group",
    title: "Group Permits",
    subtitle: "Requests",
    icon: "groups",
    iconBg: "#FFEFD6",
    iconColor: "#D97706",
    href: "/permit-group",
  },
  {
    key: "permits",
    title: "Permits",
    subtitle: "Overview",
    icon: "description",
    iconBg: "#E6F4EA",
    iconColor: Colors.light.primary,
    href: "/permits",
  },
];

export default function MarketingScreen() {
  const router = useRouter();
  const { width } = useWindowDimensions();

  const { tileSize, tileColumns } = useMemo(() => {
    const gap = 12;
    const padding = 20 * 2;
    const min = 140;

    const availableFor2 = width - padding - gap * 1;
    const size2 = Math.floor(availableFor2 / 2);
    if (size2 >= min) return { tileSize: size2, tileColumns: 2 as const };

    const availableFor1 = width - padding;
    const size1 = Math.floor(availableFor1 / 1);
    return { tileSize: Math.max(min, size1), tileColumns: 1 as const };
  }, [width]);

  const openModule = useCallback(
    async (href: string) => {
      const saved = (await AsyncStorage.getItem(API_BASE_URL_KEY)) ?? "";
      const base = runtimePortalBaseUrl(saved);
      if (!base) {
        Alert.alert("Portal not configured", "Please connect to the portal first.");
        router.push("/portal-login" as any);
        return;
      }
      router.push(href as any);
    },
    [router],
  );

  return (
    <ThemedView style={styles.container}>
      <ScrollView contentContainerStyle={styles.content} bounces={false}>
        <View style={[styles.grid, tileColumns === 1 && styles.gridOneCol]}>
          {cards.map((c) => (
            <Pressable
              key={c.key}
              onPress={() => openModule(c.href)}
              style={({ pressed }) => [
                styles.card,
                { width: tileSize, height: tileSize, opacity: pressed ? 0.9 : 1 },
              ]}
            >
              <View style={[styles.iconCircle, { backgroundColor: c.iconBg }]}>
                <MaterialIcons name={c.icon as any} size={28} color={c.iconColor} />
              </View>
              <ThemedText style={styles.cardTitle} numberOfLines={1}>
                {c.title}
              </ThemedText>
              <ThemedText style={styles.cardSub} numberOfLines={1}>
                {c.subtitle}
              </ThemedText>
            </Pressable>
          ))}
        </View>
      </ScrollView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  content: {
    padding: 20,
    paddingBottom: 28,
  },
  title: {
    fontSize: 22,
    fontFamily: FontFamily.serif,
    fontWeight: FontWeight.bold,
    color: "#111827",
  },
  subTitle: {
    marginTop: 6,
    fontSize: 12,
    fontFamily: FontFamily.serif,
    fontWeight: FontWeight.regular,
    color: "#6B7280",
  },
  grid: {
    marginTop: 16,
    flexDirection: "row",
    flexWrap: "wrap",
    justifyContent: "space-between",
    rowGap: 12,
  },
  gridOneCol: {
    justifyContent: "flex-start",
    rowGap: 12,
  },
  card: {
    borderRadius: 18,
    paddingHorizontal: 14,
    paddingTop: 16,
    paddingBottom: 14,
    backgroundColor: "#F3F4F6",
    justifyContent: "flex-start",
    alignItems: "center",
    shadowColor: "#000000",
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.06,
    shadowRadius: 10,
    elevation: 1,
    overflow: "hidden",
  },
  iconCircle: {
    width: 58,
    height: 58,
    borderRadius: 29,
    alignItems: "center",
    justifyContent: "center",
    marginBottom: 12,
  },
  cardTitle: {
    fontSize: 13,
    textAlign: "center",
    fontFamily: FontFamily.serif,
    fontWeight: FontWeight.bold,
    color: "#111827",
    lineHeight: 16,
    maxWidth: "100%",
  },
  cardSub: {
    marginTop: 4,
    fontSize: 11,
    textAlign: "center",
    fontFamily: FontFamily.serif,
    fontWeight: FontWeight.regular,
    color: "#6B7280",
    lineHeight: 14,
    maxWidth: "100%",
  },
});
