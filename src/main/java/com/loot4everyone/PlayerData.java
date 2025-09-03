package com.loot4everyone;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class PlayerData {
    private HashMap<BlockPos, List<ItemStack>> inventory = new HashMap<>();
    private List<BlockPos> elytras = new ArrayList<>();

    public HashMap<BlockPos, List<ItemStack>> getInventory() {
        return inventory;
    }

    public void addInventory(BlockPos pos, List<ItemStack> stacks){
        inventory.put(pos, stacks);
    }

    public void stringToInventory(String data) {
        if (data == null || data.isEmpty()) return;
        inventory.clear();
        String[] entries = data.split(";");
        for (String entry : entries) {
            String[] parts = entry.split("=");
            String[] posParts = parts[0].split(",");
            BlockPos pos = new BlockPos(Integer.parseInt(posParts[0]), Integer.parseInt(posParts[1]), Integer.parseInt(posParts[2]));
            List<ItemStack> stacks = new ArrayList<>();
            for (String itemData : parts[1].split(",")) {
                String[] itemParts = itemData.split("%");
                ItemStack itemStack = new ItemStack(RegistryEntry.of(Item.byRawId(Integer.parseInt(itemParts[0]))),Integer.parseInt(itemParts[1]));
                stacks.add(itemStack);
            }
            inventory.put(pos, stacks);
        }
    }

    public NbtList inventoryToNbt() {
        NbtList inventoryEntriesTag = new NbtList();

        for (Map.Entry<BlockPos, List<ItemStack>> positionEntry : inventory.entrySet()) {
            BlockPos chestPosition = positionEntry.getKey();
            List<ItemStack> itemStacksAtPosition = positionEntry.getValue();
            if (itemStacksAtPosition == null || itemStacksAtPosition.isEmpty()) continue;

            NbtCompound inventoryTag = new NbtCompound();
            inventoryTag.putLong("position", chestPosition.asLong());

            NbtList stackList = new NbtList();
            for (ItemStack itemStack : itemStacksAtPosition) {
                if (itemStack == null || itemStack.isEmpty()) continue;

                NbtCompound stackTag = new NbtCompound();
                Identifier itemIdentifier = Registries.ITEM.getId(itemStack.getItem());
                int stackCount = itemStack.getCount();

                stackTag.putString("id", itemIdentifier.toString());
                stackTag.putInt("count", stackCount);
                stackList.add(stackTag);
            }

            if (!stackList.isEmpty()) {
                inventoryTag.put("items", stackList);
                inventoryEntriesTag.add(inventoryTag);
            }
        }

        return inventoryEntriesTag;
    }

    public void inventoryFromNbt(NbtList inventoryEntriesTag) {
        this.inventory.clear();

        for (int entryIndex = 0; entryIndex < inventoryEntriesTag.size(); entryIndex++) {
            NbtCompound inventoryTag = inventoryEntriesTag.getCompound(entryIndex);

            long blockPosition = inventoryTag.getLong("position");
            BlockPos chestPosition = BlockPos.fromLong(blockPosition);

            NbtList stackList = inventoryTag.getList("items", NbtElement.COMPOUND_TYPE);
            List<ItemStack> stacks = new ArrayList<>(stackList.size());

            for (int stackIndex = 0; stackIndex < stackList.size(); stackIndex++) {
                NbtCompound stackTag = stackList.getCompound(stackIndex);

                String itemId = stackTag.getString("id");
                Identifier itemIdentifier = Identifier.tryParse(itemId);
                int stackCount = stackTag.getInt("count");
                if (itemIdentifier == null || stackCount <= 0) continue;

                Item item = Registries.ITEM.get(itemIdentifier);
                stacks.add(new ItemStack(item, stackCount));
            }

            if (!stacks.isEmpty()) {
                this.inventory.put(chestPosition, stacks);
            }
        }
    }

}
