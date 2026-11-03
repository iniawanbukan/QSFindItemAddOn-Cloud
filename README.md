# Shop Search AddOn for QuickShop
### Version: 2.0.8.2-RELEASE

**by chairawanchannel**

> **Watermark:** This documentation and build are maintained by **chairawanchannel**.

An unofficial add-on for [QuickShop-Hikari](https://www.spigotmc.org/resources/100125/) that gives your players one in-game shop browser, available through `/qss` and `/finditem`. Instead of walking around hoping to spot the right sign, players can list every shop, filter and sort the results, and teleport straight to the chest they want.

---

## What it does

`/qss` and `/finditem` now open the **same unified GUI**. Every entry in that GUI is **one QuickShop shop**, not a grouped item type. This means:

- A player can browse every shop that matches the current side, category and search.
- If the same owner creates two shops for the same item at different chests, both shops appear as separate entries.
- Clicking an entry teleports the player directly to that shop (with a configurable safety check and countdown).
- `/finditem TO_BUY diamond` opens the same GUI with the side and item filter already applied, so there is only one result GUI to learn and maintain.

No more asking in chat "does anyone sell diamonds."

---

## Getting started

1. Make sure [QuickShop-Hikari](https://www.spigotmc.org/resources/100125/) and [Vault](https://www.spigotmc.org/resources/34315/) (with an economy plugin) are installed.
2. Drop the plugin jar into your server's `plugins` folder.
3. Restart the server. A `config.yml` will be generated automatically.
4. In game, run `/qss` to browse every shop, or `/finditem` to search for a specific item.

That is it. No extra setup is required for the basic search and teleport flow. Everything else below is optional tuning.

---

## The unified shop browser

Both commands open `handlers/gui/qss/Menu.java`, which is laid out like this:

```text
[ Sort ] [   ] [ Shops ] [   ] [ Category ] [ ] [ ] [ ] [ Search ]
[ shop ] [ shop ] [ shop ] [ shop ] [ shop ] [ shop ] [ shop ] [ shop ] [ shop ]
[ shop ] [ shop ] [ shop ] [ shop ] [ shop ] [ shop ] [ shop ] [ shop ] [ shop ]
[ shop ] [ shop ] [ shop ] [ shop ] [ shop ] [ shop ] [ shop ] [ shop ] [ shop ]
[ shop ] [ shop ] [ shop ] [ shop ] [ shop ] [ shop ] [ shop ] [ shop ] [ shop ]
[ Prev ] [   ] [   ] [ Page info ] [ Close ] [   ] [   ] [   ] [ Next ]
```

- **36 shops per page**
- **One shop per slot**
- **Click a shop entry to teleport there**
- **Previous / Next only** — there are no first-page or last-page buttons
- **No "No items found" barrier** — the page simply stays empty and the page info shows zero shops

### Shop entries

Each shop entry uses the shop's actual item, keeps the item's metadata (display name, lore, enchantments, potion effects and custom model data), and shows configurable lore such as price, stock or space, owner, location, world, nearest warp and visit count.

Multiple shops for the same item are intentionally **not merged**. The menu uses `inventory.setItem(...)` for every shop, so two diamond shops owned by the same player, even at the same price, remain two separate entries. Each entry carries its own chest coordinates, so clicking one teleports to the correct chest.

### Top control row

| Slot | Control | Left-click | Right-click |
|---|---|---|---|
| 1 | **Sort** | Next sort mode | Previous sort mode |
| 3 | **Shops** | Switch `TO_BUY` / `TO_SELL` | Switch `TO_BUY` / `TO_SELL` |
| 5 | **Category** | Next category | Previous category |
| 9 | **Search** | Open the sign search | Clear the current search |

### Bottom row

| Slot | Control | Action |
|---|---|---|
| 45 | Previous Page | Go to the previous page |
| 48 | Page Info | Shows shop count, side, sort, category and search |
| 49 | Close | Close the browser |
| 53 | Next Page | Go to the next page |

---

## Commands

### `/qss`

Opens the unified shop browser with its default filters.

- Permission: `finditem.use`
- Default side: `TO_BUY`
- Default sort: `Best Price`
- Default category: `All`
- Default search: none (all shops are shown)

### `/finditem`

`/finditem` now routes into the same GUI as `/qss`.

- `/finditem TO_BUY <item>`: opens the GUI on the `TO_BUY` side, filtered to the item.
- `/finditem TO_SELL <item>`: opens the GUI on the `TO_SELL` side, filtered to the item.
- `/finditem TO_BUY *` or `/finditem TO_SELL *`: opens the GUI with no item filter.
- `/finditem hideshop`: while looking at a shop chest, hides that shop from search results.
- `/finditem revealshop`: makes a previously hidden shop searchable again.
- `/finditem` on its own shows the command list in game.

The `TO_BUY` / `TO_SELL` labels describe what the **player wants to do**:

- **`TO_BUY`** — "I want to buy." Shows shops that **sell** the item, so the player can buy from them.
- **`TO_SELL`** — "I want to sell." Shows shops that **buy** the item, so the player can sell to them.

By default `/finditem` also works as `/searchshop`, `/shopsearch`, and `/searchitem`. You can change or remove these aliases in `config.yml` under `find-item-command.command-alias`.

---

## Side filter (`TO_BUY` / `TO_SELL`)

The side control narrows the list to one half of the QuickShop market:

- **`TO_BUY`**: shops that sell items to players. Best for players who want to buy something.
- **`TO_SELL`**: shops that buy items from players. Best for players who want to sell something.

Each side is cached after it loads. Switching back to a side that was already loaded does not query QuickShop again. The control at slot 3 uses left-click or right-click to toggle between the two sides.

---

## Sorting

The sort button cycles through five modes. Left-click goes forward, right-click goes backward.

| Sort mode | What it does | Best for |
|---|---|---|
| **Best Price** | Side-aware pricing. `TO_BUY` shows the lowest price first. `TO_SELL` shows the highest payout first. | The default, most useful view |
| **Price: Lowest** | Always sorts from the cheapest shop to the most expensive | Finding the cheapest listing regardless of side |
| **Price: Highest** | Always sorts from the most expensive shop to the cheapest | Checking the top end of the market |
| **Stock: Highest** | Sorts by remaining stock or space, highest first. Unlimited shops rank above finite shops, unknown values rank last | Finding shops that can handle a large trade |
| **Recently Listed** | Sorts by QuickShop shop ID, newest first | Finding newly created shops |

Every sort has a stable tie-breaker using the shop ID and chest location, so the list does not jump around when several shops share the same price or stock.

---

## Categories

The category filter is applied to each shop's item material:

- All
- Blocks
- Tools
- Food
- Combat
- Potions
- Books
- Ingredients
- Utilities

Left-click advances to the next category, right-click goes back. Categories work together with the side filter and the search box, so a player can, for example, show only `TO_BUY` shops in `Blocks` whose name contains "deepslate."

---

## Search

The search control at slot 9 opens a temporary sign near the player:

- Type any part of an item name and submit the sign.
- The search matches both the material name and the item's custom display name.
- Leaving the sign blank clears the search and shows every shop again.
- Right-clicking the search button clears the search without opening a sign.

`/finditem <material>` applies an exact material filter when the argument is a valid material name; otherwise it is used as a free-text search. Both paths open the same GUI, so the player can immediately refine the result with the controls above.

---

## Teleportation

Clicking a shop entry attempts to teleport the player to that shop.

### Direct teleport

Direct shop teleport is controlled by:

```yaml
player-shop-teleportation:
  direct-shop-tp-mode:
    tp-player-directly-to-shop: true
```

When enabled, the plugin stores the shop's exact chest coordinates on the GUI item. Clicking the entry parses those coordinates, finds a safe landing spot around the shop, and teleports the player.

### 3-second countdown

By default, the teleport now uses a visible **3-second countdown**:

```yaml
player-shop-teleportation:
  direct-shop-tp-mode:
    tp-delay-in-seconds: 3
    tp-delay-message: "&6Teleporting to the shop in &c{DELAY} &6second(s)..."
```

The player sees:

```text
Teleporting to the shop in 3 second(s)...
Teleporting to the shop in 2 second(s)...
Teleporting to the shop in 1 second(s)...
→ teleport
```

`{DELAY}` is replaced with the remaining seconds. Set `tp-delay-in-seconds: 0` if you want instant teleport for everyone instead.

### Countdown bypass permission

Players with this permission skip the countdown and teleport instantly:

```text
finditem.shoptp-delay.bypass
```

`finditem.admin` also bypasses the countdown. Example LuckPerms command:

```text
/lp group vip permission set finditem.shoptp-delay.bypass true
```

The base teleport permission is separate:

```text
finditem.shoptp
```

Its default is `op`, so remember to grant it to normal players if they should be able to teleport at all:

```text
/lp group default permission set finditem.shoptp true
```

### Other teleport rules

- **Own shops:** teleporting to your own shop is controlled by `finditem.shoptp.own` (default: everyone). Server owners who do not want this can deny the permission.
- **Safe landing:** the plugin checks that the landing area around the shop is safe. Players with `finditem.shoptp.bypass-safetycheck` can skip this check.
- **Unsafe area message:** configurable under `unsafe-shop-area-message`.
- **No permission message:** configurable under `shop-tp-no-permission-message`.
- **Warp mode:** if direct teleport is disabled and `tp-player-to-nearest-warp` is enabled, clicking a shop sends the player to the nearest configured warp instead.
- **Custom commands:** if enabled, clicking a shop can run your own commands with `{PLAYER_NAME}`, `{SHOP_LOC_X}`, `{SHOP_LOC_Y}`, `{SHOP_LOC_Z}` and `{SHOP_WORLD}` placeholders.

---

## Permissions

| Permission | Default | What it allows |
|---|---|---|
| `finditem.use` | everyone | Use `/finditem` and `/qss` |
| `finditem.hideshop` | everyone | Use `/finditem hideshop` and `/finditem revealshop` |
| `finditem.shoptp` | op | Teleport directly to a shop (only matters when direct teleport is enabled) |
| `finditem.shoptp.own` | everyone | Teleport to your own shops |
| `finditem.shoptp-delay.bypass` | op | Skip the teleport countdown and teleport instantly |
| `finditem.shoptp.bypass-safetycheck` | nobody | Skip the safe-landing check |
| `finditem.reload` | op | Use `/finditemadmin reload` |
| `finditem.admin` | op | Use all admin commands and bypass the countdown |

---

## Admin commands

`/finditemadmin` (alias `/fiadmin`) is for server staff:

- `/finditemadmin reload`: reloads `config.yml` without restarting the server
- `/finditemadmin debug-mode {enable|disable}`: turns on extra logging if you are troubleshooting an issue and want more detail in the console

---

## Features in detail

### Discovery and search

- Browse every QuickShop shop from one GUI.
- Search by exact material or by a free-text item name.
- Matches custom items by their display name as well as the base material, and keeps custom model data on the GUI entry.
- Enchantments and potion effects, including custom colors, are shown in the shop lore.
- Enchant glow is hidden automatically if the item has the "hide enchants" flag.
- Optionally limit results to a distance from the player with `shop-search-max-distance`.
- Optionally restrict searches to already-loaded chunks for lower server load.
- Shops with no stock can be excluded by the existing search rules so players do not waste a trip.

### Shop listing

- One GUI entry per shop.
- Duplicate shops for the same item are preserved.
- Duplicate shops from the same owner are preserved.
- Each entry carries its own chest coordinates.
- Clicking an entry teleports to that specific shop.
- Pagination uses previous/next only.
- No first-page/last-page buttons.
- No empty-state barrier.

### Filters and sorting

- Side filter: `TO_BUY` / `TO_SELL`.
- Category filter: blocks, tools, food, combat, potions, books, ingredients, utilities.
- Sign-based search.
- Sort modes: Best Price, Price: Lowest, Price: Highest, Stock: Highest, Recently Listed.
- Side-aware Best Price sorting.
- Stable tie-breaking by shop ID and chest location.

### Teleportation

- Safe direct teleportation to the shop chest.
- Visible 3-second countdown by default.
- `finditem.shoptp-delay.bypass` for instant teleport.
- Own-shop permission control.
- Safe-landing check with a bypass permission.
- Optional nearest-warp mode using PlayerWarps, EssentialsX, WorldGuard regions or Residence.
- Optional custom commands on shop click.

### Privacy and server control

- Shop owners can hide their own shop with `/finditem hideshop` and unhide it with `/finditem revealshop`.
- Block specific worlds from appearing in search results.
- Block specific materials from being searchable at all.
- Staff can use `/finditemadmin reload` to apply config changes.
- Works on Paper and Folia servers.
- BentoBox locked-island shops are excluded.

### Customization

- Every message in `config.yml` supports legacy and hex color codes.
- Command aliases, GUI titles, button materials and lore text are editable.
- Large prices can be shortened automatically, for example `$10,210,100` shown as `$10.21M`.
- Optional shop visit counter with the `{SHOP_VISITS}` placeholder.
- Item lore supports `{ITEM_PRICE}`, `{SHOP_STOCK}`, `{SHOP_OWNER}`, `{SHOP_LOC}`, `{SHOP_WORLD}`, `{NEAREST_WARP}` and `{SHOP_VISITS}`.

---

## What changed in this build

This build focuses on removing the duplicated result screens and making the shop list itself the main experience.

### 1. Unified `/qss` and `/finditem` GUI

Before, `/finditem TO_BUY diamond` opened a separate shop-result menu that looked different from `/qss`. Both entry points now open the same `handlers/gui/qss/Menu.java` browser.

### 2. `/qss` now lists shops directly

The old flow required clicking an item first and then opening a second menu with the shops for that item. The new flow lists shops immediately:

```text
/qss → shop list → click a shop → teleport
```

### 3. Duplicate shops are preserved

Every QuickShop is kept as its own entry. If one owner creates two diamond shops at different chests, both appear separately, with their own prices and locations. The GUI uses `inventory.setItem(...)` per slot, so identical item stacks never merge.

### 4. Click-to-teleport

Clicking a shop entry teleports the player to that shop's chest. The old shop-list logic, including safe-location checks, visit tracking, Essentials last-location updates, warp fallback and custom commands, was kept.

### 5. `TO_BUY` / `TO_SELL` filter

A new side control at slot 3 switches between the two sides of the market. Each side is cached after loading, so switching back is instant. The filter matches the existing `/finditem` side mapping.

### 6. New sorting modes

Added:

- `Best Price` (side-aware)
- `Stock: Highest`

Kept/standardized:

- `Price: Lowest`
- `Price: Highest`
- `Recently Listed`

### 7. Category and sign search retained

The category filter and sign-based search from the previous `/qss` work were carried into the unified browser.

### 8. Pagination cleanup

- Removed first-page and last-page buttons.
- Removed the "No items found" barrier.
- Kept previous/next navigation, page information and close.

### 9. Teleport countdown and bypass permission

- Default countdown is now 3 seconds.
- The player receives one countdown message per second.
- `finditem.shoptp-delay.bypass` skips the countdown.
- `finditem.admin` also skips the countdown.
- `tp-delay-in-seconds: 0` disables the countdown for everyone.

### 10. Config migration

`config-version` is now `23`. Existing configs below version 23 are migrated automatically:

- If `tp-delay-in-seconds` is missing, `0` or negative, it is set to `3`.
- An explicit non-zero value is preserved.

### 11. Internal cleanup

- `handlers/gui/menus/FoundShopsMenu.java` was removed; its shop-list functionality moved to `handlers/gui/qss/Menu.java`.
- `handlers/gui/menus/QssItemShopsMenu.java` was removed.
- `handlers/gui/menus/QssMainMenu.java` was removed after the unified browser replaced the two-step item flow.
- `models/QssItemEntry.java` was removed because shops are no longer grouped by item type.
- `QssSession` now stores the raw `List<FoundShopItemModel>` per side and exposes `getVisibleShops()`.
- `QssSortMode` now compares `FoundShopItemModel` directly.
- `QssSessionManager` opens `qss/Menu` for both `/qss` and `/finditem`.
- `FoundShopItemModel` now carries QuickShop's `shopId` for the "Recently Listed" sort.

### 12. Documentation and permissions

- Updated `/qss` and `finditem.use` descriptions in `plugin.yml`.
- Updated the countdown bypass permission description.
- Rewrote this README with detailed feature documentation, the change list and the `by chairawanchannel` watermark.

---

## Configuration notes

All settings live in `config.yml`, which is generated the first time the plugin starts. A fully annotated sample, with every option explained, is available on the [wiki](https://github.com/myzticbean/QSFindItemAddOn/wiki/Sample-config.yml).

Useful options for the new flow:

```yaml
shop-search-gui-title: "&l» &rShops"

player-shop-teleportation:
  direct-shop-tp-mode:
    tp-player-directly-to-shop: true
    tp-delay-in-seconds: 3
    tp-delay-message: "&6Teleporting to the shop in &c{DELAY} &6second(s)..."
    click-to-teleport-message: "&6&lClick to teleport to the shop!"
    shop-tp-no-permission-message: "&cYou don't have permission to teleport to shop!"
    unsafe-shop-area-message: "&cThe area around the shop is unsafe!"

shop-search-max-distance: 0
```

When you update the plugin, new config options are added automatically to your existing `config.yml` the next time the server starts, so you will not lose your existing settings.

---

## Testing checklist

After replacing the jar and restarting the server:

1. Run `/qss` and confirm the shop list opens directly.
2. Click any shop entry and confirm the countdown appears, then the player teleports to that chest.
3. Create two shops for the same item with the same owner at different chests and confirm both appear as separate entries.
4. Click each duplicate shop and confirm each teleports to the correct chest.
5. Use the `Shops` control to switch between `TO_BUY` and `TO_SELL`.
6. Cycle through every sort mode and confirm the order changes.
7. Cycle through categories.
8. Left-click the search button, type an item name on the sign, and confirm the list filters.
9. Right-click the search button and confirm the filter clears.
10. Run `/finditem TO_BUY diamond` and confirm it opens the same GUI with the filter applied.
11. Grant `finditem.shoptp-delay.bypass` to a test player and confirm teleport becomes instant.
12. Set `tp-delay-in-seconds: 0`, run `/finditemadmin reload`, and confirm the countdown is disabled for everyone.

---

## Demo

The screenshots below are from the earlier item-search GUI. They still show how items, enchantments, potion effects and custom names are rendered; the current build uses the unified shop browser described above.

**Searching for an item:**

![/finditem_usage](https://cdn.modrinth.com/data/asp13ugE/images/bb37966809c9d7ab3201988ef58b2060688584f3.png)
![alt text](https://cdn.modrinth.com/data/asp13ugE/images/878e9b703343a65c963d790d875ad5dbe6ac309d.png)

**Multiple result pages:**

![alt text](https://cdn.modrinth.com/data/asp13ugE/images/33cb7d96cabb709bc630685c9e6fdc1b9cd7b3bb.png)

**Enchantments shown in the listing:**

![alt text](https://cdn.modrinth.com/data/asp13ugE/images/8ac5643bc042b897e549400e29186d87024b3a71.png)

**Potion colors and effects shown in the listing:**

![alt text](https://cdn.modrinth.com/data/asp13ugE/images/786ce10d42c5e92cbbd12b7f1ee81011796acbe0.png)

**Custom item names and lore preserved:**

![alt text](https://cdn.modrinth.com/data/asp13ugE/images/0c30b767bfc9df1f4a79afef677c0fc262fa62c5.png)

---

## Integrations

The plugin picks these up automatically if they are installed, no extra configuration needed beyond what is in `config.yml`:

- **[PlayerWarps](https://www.spigotmc.org/resources/66692/)**: shows the nearest warp to each shop in the search results
- **EssentialsX Warps**: same idea, using Essentials warps instead. The warp list refreshes every 15 minutes in the background to keep searches fast. If you just added a warp and do not want to wait, run `/finditemadmin reload`
- **WorldGuard**: can show which region a shop is in (if regions overlap, the highest priority one is used)
- **Residence**: can show which residence, including subzones, a shop is in
- **BentoBox**: shops on locked islands are excluded from search results

None of these are required. The plugin works fine with just QuickShop-Hikari and Vault installed.

---

## Requirements

- [QuickShop-Hikari](https://www.spigotmc.org/resources/100125/) v6.3.0.0 or higher
- [Vault](https://www.spigotmc.org/resources/34315/) with a compatible economy plugin
- Paper 1.20 or higher (Folia is supported too)

**Note:** support for the older QuickShop Reremake fork has been dropped completely. If you are still on Reremake, this plugin will not work for you.

---

## Building from source

```bash
mvn -DskipTests package
```

The shaded jar is written to:

```text
target/QSFindItemAddOn-2.0.8.2-RELEASE-shaded.jar
```

---

## Getting help / reporting a bug

If something is not working, please [open an issue](https://github.com/myzticbean/QSFindItemAddOn/issues) and include as much detail as you can, ideally with your console log attached. That same page also lists what is currently being worked on.

---

## Contributing

Pull requests are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) for how to set up a build and the branch naming convention we use.

---

## Metrics

We use bStats to see how many servers are running the plugin and on what versions, which helps prioritize what to support. No personal data is collected.

[![BigImage](https://bstats.org/signatures/bukkit/QSFindItemAddOn.svg)](https://bstats.org/plugin/bukkit/QSFindItemAddOn/12382)

---

## Ringkasan Perubahan (Bahasa Indonesia)

**by chairawanchannel**

Perubahan utama pada build ini:

1. **`/qss` dan `/finditem` memakai GUI yang sama.** `/finditem TO_BUY diamond` sekarang membuka browser shop yang sama dengan `/qss`, bukan menu hasil pencarian terpisah.
2. **`/qss` langsung menampilkan daftar shop.** Satu entry = satu QuickShop, bukan satu jenis item.
3. **Shop duplikat tetap tampil terpisah.** Kalau satu pemain membuat dua shop diamond di chest berbeda, keduanya muncul dengan harga dan lokasi masing-masing.
4. **Klik entry shop langsung teleport.** Teleport tetap memakai pengecekan lokasi aman, pencatatan kunjungan, Essentials last location, fallback warp dan custom command yang lama.
5. **Filter `TO_BUY` / `TO_SELL`.** Data per sisi di-cache, jadi berpindah sisi tidak memanggil QuickShop berulang kali.
6. **Sort baru dan lebih berguna.** `Best Price`, `Price: Lowest`, `Price: Highest`, `Stock: Highest`, dan `Recently Listed`. `Best Price` otomatis memilih harga terendah untuk `TO_BUY` dan harga tertinggi untuk `TO_SELL`.
7. **Filter kategori dan pencarian lewat sign.** Klik kiri sign untuk mencari, klik kanan untuk menghapus pencarian.
8. **Pagination dibersihkan.** Tombol first page dan last page dihapus, begitu juga barrier "No items found". Navigasi sekarang hanya previous/next.
9. **Countdown teleport 3 detik.** Pemain melihat pesan hitung mundur 3, 2, 1 sebelum teleport.
10. **Permission bypass countdown.** `finditem.shoptp-delay.bypass` membuat teleport instan. `finditem.admin` juga otomatis bypass.
11. **Config version 23.** Config lama otomatis dimigrasikan; jika `tp-delay-in-seconds` masih `0`, nilainya diubah menjadi `3`.
12. **Refactor internal.** `FoundShopsMenu`, `QssItemShopsMenu`, `QssMainMenu`, dan `QssItemEntry` dihapus. Logika daftar shop sekarang ada di `handlers/gui/qss/Menu.java`, dan `QssSession` menyimpan daftar shop mentah per sisi.

Catatan permission:

- `finditem.use` — membuka `/qss` dan `/finditem`
- `finditem.shoptp` — izin dasar untuk teleport ke shop (default `op`)
- `finditem.shoptp-delay.bypass` — melewati countdown (default `op`)
- `finditem.shoptp.own` — boleh teleport ke shop sendiri (default semua pemain)
- `finditem.shoptp.bypass-safetycheck` — melewati pengecekan lokasi aman

---

**Watermark: by chairawanchannel**
