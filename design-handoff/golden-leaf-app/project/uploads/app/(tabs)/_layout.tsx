import { Tabs, useRouter } from "expo-router";
import { useNavigation } from "@react-navigation/native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import React from "react";
import { Platform, Pressable, StyleSheet, View } from "react-native";
import MaterialIcons from "@expo/vector-icons/MaterialIcons";

import { HapticTab } from "@/components/haptic-tab";
import { IconSymbol } from "@/components/ui/icon-symbol";
import { Colors } from "@/constants/theme";
import { useColorScheme } from "@/hooks/use-color-scheme";

export default function TabLayout() {
  const colorScheme = useColorScheme();
  const theme = colorScheme === "dark" ? "dark" : "light";
  const router = useRouter();
  const navigation = useNavigation();
  const insets = useSafeAreaInsets();
  
  const bottomPadding = Platform.OS === 'ios' ? insets.bottom || 28 : insets.bottom + 10;
  const tabHeight = Platform.OS === 'ios' ? 60 + bottomPadding : 54 + bottomPadding;

  return (
    <Tabs
      screenOptions={({ route }) => ({
        tabBarActiveTintColor: Colors[theme].tint,
        tabBarInactiveTintColor: Colors[theme].muted,
        tabBarStyle: {
          backgroundColor: Colors[theme].surface,
          borderTopWidth: 0,
          height: tabHeight,
          paddingBottom: bottomPadding,
          paddingTop: 8,
          elevation: 12,
          shadowColor: "#000000",
          shadowOpacity: 0.06,
          shadowRadius: 12,
          shadowOffset: { width: 0, height: -6 },
        },
        tabBarItemStyle: {
          paddingTop: 2,
        },
        tabBarLabelStyle: {
          fontSize: 11,
          fontWeight: '500',
        },
        tabBarButton: (props) => <HapticTab {...props} />,
        headerStyle: {
          backgroundColor: Colors[theme].primary,
        },
        headerTintColor: Colors[theme].surface,
        headerTitleAlign: "left",
        headerTitleStyle: { fontSize: 22, fontWeight: "600", color: Colors[theme].surface },
        headerShadowVisible: false,
        headerShown: true,
        headerLeft: route.name !== "index" ? () => (
          <Pressable
            onPress={() => (navigation.canGoBack() ? navigation.goBack() : router.replace("/"))}
            style={{ paddingHorizontal: 16, paddingVertical: 8 }}
            hitSlop={10}
          >
            <MaterialIcons name="arrow-back" size={24} color={Colors[theme].surface} />
          </Pressable>
        ) : undefined,
      })}
    >
      <Tabs.Screen
        name="index"
        options={{
          title: "Home",
          headerShown: false,
          tabBarIcon: ({ focused, color }) => (
            <View style={[styles.iconPill, focused && { backgroundColor: Colors[theme].primary }]}>
              <IconSymbol size={26} name="house.fill" color={focused ? Colors[theme].surface : color} />
            </View>
          ),
        }}
      />
      <Tabs.Screen
        name="explore"
        options={{
          title: "Search",
          tabBarLabel: "Search",
          headerShown: false,
          tabBarIcon: ({ focused, color }) => (
            <View style={[styles.iconPill, focused && { backgroundColor: Colors[theme].primary }]}>
              <IconSymbol size={26} name="magnifyingglass" color={focused ? Colors[theme].surface : color} />
            </View>
          ),
        }}
      />
      <Tabs.Screen
        name="registration"
        options={{
          title: "Growers",
          tabBarLabel: "Growers",
          headerShown: false,
          tabBarIcon: ({ focused, color }) => (
            <View style={[styles.iconPill, focused && { backgroundColor: Colors[theme].primary }]}>
              <IconSymbol size={26} name="person.badge.plus" color={focused ? Colors[theme].surface : color} />
            </View>
          ),
        }}
      />
      <Tabs.Screen
        name="marketing"
        options={{
          title: "Sales",
          tabBarLabel: "Sales",
          tabBarIcon: ({ focused, color }) => (
            <View style={[styles.iconPill, focused && { backgroundColor: Colors[theme].primary }]}>
              <IconSymbol size={26} name="chart.line.uptrend.xyaxis" color={focused ? Colors[theme].surface : color} />
            </View>
          ),
        }}
      />
      {/* Hide the remaining screens from the tab bar but keep them accessible inside (tabs) layout */}
      <Tabs.Screen name="inspection" options={{ href: null, headerShown: false }} />
      <Tabs.Screen name="permits" options={{ href: null, title: "Permits" }} />
      <Tabs.Screen name="arbitration" options={{ href: null, title: "Arbitration" }} />
      <Tabs.Screen name="renewal" options={{ href: null, title: "Renewal" }} />
      <Tabs.Screen name="validation" options={{ href: null, title: "Validation" }} />
    </Tabs>
  );
}

const styles = StyleSheet.create({
  iconPill: {
    width: 52,
    height: 34,
    borderRadius: 18,
    alignItems: "center",
    justifyContent: "center",
  },
});
