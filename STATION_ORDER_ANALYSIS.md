# Station Order Analysis

## Current Order (WRONG!)

### VegagerdinProvider stations (5 stations):
1. Hafnarfjall (64.4755°N)
2. Brattabrekka (64.8716°N)
3. Þröskuldar (65.5524°N)
4. Steingrímsfjarðarheiði (65.7503°N)
5. Ögur (66.0449°N)

### VedurAwsProvider stations (3 stations):
1. Reykjavík (64.1275°N) ← START
2. Hólmavík (65.6873°N)
3. Ísafjörður (66.0596°N) ← END

### Current concatenation result:
```java
Stream.concat(vegagerdin.listStations().stream(), vedur.listStations().stream())
```

**Resulting order**:
1. Hafnarfjall (veg) - 64.4755°N ❌ WRONG!
2. Brattabrekka (veg) - 64.8716°N
3. Þröskuldar (veg) - 65.5524°N
4. Steingrímsfjarðarheiði (veg) - 65.7503°N
5. Ögur (veg) - 66.0449°N
6. Reykjavík (vedur) - 64.1275°N ❌ Should be FIRST!
7. Hólmavík (vedur) - 65.6873°N
8. Ísafjörður (vedur) - 66.0596°N

## Correct Order (by latitude, south to north):

1. **Reykjavík** (vedur) - 64.1275°N ← START
2. **Hafnarfjall** (veg) - 64.4755°N
3. **Brattabrekka** (veg) - 64.8716°N
4. **Þröskuldar** (veg) - 65.5524°N
5. **Hólmavík** (vedur) - 65.6873°N
6. **Steingrímsfjarðarheiði** (veg) - 65.7503°N
7. **Ögur** (veg) - 66.0449°N
8. **Ísafjörður** (vedur) - 66.0596°N ← END

## Problem

The current code just concatenates the two lists, which means:
- All Vegagerdin stations come first
- All Vedur stations come after
- This is NOT the correct route order!

## Solution

We need to **merge and sort by latitude** to get the correct order!

