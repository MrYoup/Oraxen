package io.th0rgal.oraxen.command.commands;

import static java.util.Arrays.stream;

import static io.th0rgal.oraxen.command.argument.ArgumentHelper.*;
import static io.th0rgal.oraxen.command.argument.CompletionHelper.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.th0rgal.oraxen.recipes.CustomRecipe;
import io.th0rgal.oraxen.recipes.listeners.RecipesEventsManager;
import io.th0rgal.oraxen.utils.recipeshowcase.RecipeShowcase;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.syntaxphoenix.syntaxapi.command.ArgumentType;
import com.syntaxphoenix.syntaxapi.command.Arguments;
import com.syntaxphoenix.syntaxapi.command.BaseArgument;
import com.syntaxphoenix.syntaxapi.command.DefaultCompletion;
import com.syntaxphoenix.syntaxapi.command.arguments.StringArgument;
import com.syntaxphoenix.syntaxapi.utils.java.Arrays;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.command.CommandInfo;
import io.th0rgal.oraxen.command.MinecraftInfo;
import io.th0rgal.oraxen.command.OraxenCommand;
import io.th0rgal.oraxen.command.argument.RecipeType;
import io.th0rgal.oraxen.command.condition.Conditions;
import io.th0rgal.oraxen.command.permission.OraxenPermission;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.language.Message;
import io.th0rgal.oraxen.recipes.builders.FurnaceBuilder;
import io.th0rgal.oraxen.recipes.builders.RecipeBuilder;
import io.th0rgal.oraxen.recipes.builders.ShapedBuilder;
import io.th0rgal.oraxen.recipes.builders.ShapelessBuilder;
import io.th0rgal.oraxen.utils.general.Placeholder;
import io.th0rgal.oraxen.utils.input.InputProvider;

public class Recipe extends OraxenCommand {

    public static final OraxenCommand COMMAND = new Recipe();

    public static CommandInfo info() {
        return new CommandInfo("recipe", COMMAND).setDescription("Recipes related commands");
    }

    private Recipe() {
    }

    @Override
    public void execute(MinecraftInfo info, Arguments arguments) {

        CommandSender sender = info.getSender();

        if (Conditions
            .mixed(Conditions.reqPerm(OraxenPermission.COMMAND_RECIPE), Conditions.player(Message.NOT_PLAYER))
            .isFalse(sender)) {
            return;
        }

        if (arguments.count() < 2) {
            info.getInfo().sendSimple(sender, info.getLabel());
            return;
        }

        Optional<String> option0 = restrict(get(arguments, 1, ArgumentType.STRING).map(BaseArgument::asString),
            "builder", "save", "show");
        if (!option0.isPresent()) {
            info.getInfo().sendSimple(sender, info.getLabel());
            return;
        }

        Player player = (Player) sender;

        switch (option0.get()) {

        case "builder":
            if (Conditions.reqPerm(OraxenPermission.COMMAND_RECIPE_EDIT).isFalse(sender))
                return;
            RecipeBuilder recipe0 = RecipeBuilder.get(player.getUniqueId());

            Optional<RecipeType> option01 = get(arguments, 2, RecipeType::fromArgument);
            if (!option01.isPresent()) {
                info.getInfo().sendSimple(sender, info.getLabel());
                return;
            }

            switch (option01.get()) {
            case SHAPED:
                (recipe0 != null ? recipe0 : new ShapedBuilder(player)).open();
                break;
            case SHAPELESS:
                (recipe0 != null ? recipe0 : new ShapelessBuilder(player)).open();
                break;
            case FURNACE:
                recipe0 = recipe0 != null ? recipe0 : new FurnaceBuilder(player);
                Optional<Boolean> option02 = restrict(
                    get(arguments, 3, ArgumentType.STRING).map(BaseArgument::asString), "cookingtime", "experience")
                        .map(value -> value.equals("cookingtime"));
                if (option02.isPresent()) {
                    if (recipe0 instanceof FurnaceBuilder) {
                        FurnaceBuilder furnace = (FurnaceBuilder) recipe0;
                        InputProvider input = OraxenPlugin.get().getInputProvider();
                        if (option02.get()) {
                            if (input.hasMultipleLines()) {
                                input
                                    .setMessage(String
                                        .join(InputProvider.LINE, "200", "Please enter the", "Cooking time",
                                            "(Default is: 200)"));
                            } else {
                                input.setMessage("Please enter the Cooking time (Default is: 200)");
                            }
                            furnace.setCookingTimeProvider(input);
                        } else {
                            if (input.hasMultipleLines()) {
                                input
                                    .setMessage(String
                                        .join(InputProvider.LINE, "200", "Please enter the", "Experience amount",
                                            "(Default is: 200)"));
                            } else {
                                input.setMessage("Please enter the Experience amount (Default is: 200)");
                            }
                            furnace.setExperienceProvider(input);
                        }
                        input.open(player);
                    } else {
                        Message.COMMAND_RECIPE_NO_FURNACE.send(sender);
                    }
                    return;
                }
                recipe0.open();
                break;
            }
            break;

        case "save":
            if (Conditions.reqPerm(OraxenPermission.COMMAND_RECIPE_EDIT).isFalse(sender))
                return;
            RecipeBuilder recipe = RecipeBuilder.get(player.getUniqueId());
            if (recipe == null) {
                Message.COMMAND_RECIPE_NO_BUILDER.send(sender);
                return;
            }

            Optional<String> option1 = get(arguments, 2, ArgumentType.STRING)
                .map(argument -> argument.asString().getValue());
            if (!option1.isPresent()) {
                info.getInfo().sendSimple(sender, info.getLabel());
                return;
            }

            String name = option1.get();
            String permission = get(arguments, 3, ArgumentType.STRING)
                .map(argument -> argument.asString().getValue())
                .orElse(null);

            if (permission == null)
                recipe.saveRecipe(name);
            else
                recipe.saveRecipe(name, permission);
            Message.COMMAND_RECIPE_SAVE.send(sender, Placeholder.of("name", name));
            break;

        case "show":
            List<CustomRecipe> recipes = RecipesEventsManager.get().getPermittedRecipes(player);
            if (recipes.isEmpty()) {
                Message.COMMAND_RECIPE_NO_RECIPES.send(sender);
                return;
            }
            String[] oraxenItems = OraxenItems.nameArray();
            String recipeName = restrict(get(arguments, 2, ArgumentType.STRING).map(BaseArgument::asString),
                Arrays.merge(String[]::new, oraxenItems, "all", "result", "ingredient", "name")).orElse("");
            switch (recipeName) {
            case "all":
                break;
            case "result":
                Optional<ItemStack> option000 = get(arguments, 3, argument -> generalItem(argument));
                if (!option000.isPresent()) {
                    Message.COMMAND_RECIPE_NO_ITEM.send(sender);
                    return;
                }
                ItemStack filterItem0 = option000.get();
                recipes = recipes
                    .stream()
                    .filter(customRecipe -> customRecipe.getResult().isSimilar(filterItem0))
                    .collect(Collectors.toList());
                break;
            case "ingredient":
                Optional<ItemStack> option001 = get(arguments, 3, argument -> generalItem(argument));
                if (!option001.isPresent()) {
                    Message.COMMAND_RECIPE_NO_ITEM.send(sender);
                    return;
                }
                ItemStack filterItem1 = option001.get();
                recipes = recipes
                    .stream()
                    .filter(customRecipe -> customRecipe
                        .getIngredients()
                        .stream()
                        .filter(itemStack -> itemStack != null)
                        .anyMatch(itemStack -> itemStack.isSimilar(filterItem1)))
                    .collect(Collectors.toList());
                break;
            case "name":
                String specifiedName = get(arguments, 3, ArgumentType.STRING)
                    .map(argument -> argument.asString().getValue())
                    .orElse(null);
                recipes = recipes
                    .stream()
                    .filter(customRecipe -> customRecipe.getName().equals(specifiedName))
                    .collect(Collectors.toList());
                break;
            default:
                info.getInfo().sendSimple(sender, info.getLabel());
                return;
            }
            if (recipes.isEmpty()) {
                Message.COMMAND_RECIPE_NO_RECIPES.send(sender);
                return;
            }
            new RecipeShowcase(0, recipes).open(player);
            break;
        default:
            break;
        }

    }

    @Override
    public DefaultCompletion complete(MinecraftInfo info, Arguments arguments) {
        DefaultCompletion completion = new DefaultCompletion();
        CommandSender sender = info.getSender();

        if (Conditions
            .mixed(Conditions.hasPerm(OraxenPermission.COMMAND_RECIPE), Conditions.player())
            .isFalse(sender)) {
            return completion;
        }

        boolean edit = Conditions.hasPerm(OraxenPermission.COMMAND_RECIPE_EDIT).isTrue(sender);

        int count = arguments.count();

        if (count == 1) {
            completion(completion, edit ? (new String[] { "builder", "save", "show" }) : (new String[] { "show" }));
        } else if (count == 2 || count == 3) {
            Optional<String> option0 = restrict(get(arguments, 1, ArgumentType.STRING).map(BaseArgument::asString),
                edit ? (new String[] { "builder", "save", "show" }) : (new String[] { "show" }));
            if (!option0.isPresent())
                return completion;
            switch (option0.get()) {
            case "builder":
                if (!edit)
                    break;
                if (count == 3) {
                    Optional<Boolean> option1 = get(arguments, 2, RecipeType::fromArgument)
                        .map(type -> type == RecipeType.FURNACE);
                    if (!option1.orElse(false))
                        break;
                    completion(completion, "cookingtime", "experience");
                    break;
                }
                RecipeType[] types = RecipeType.values();
                for (RecipeType type : types)
                    completion.add(new StringArgument(type.name()));
                break;
            case "save":
                if (!edit)
                    break;
                if (count == 3) {
                    completion.add(new StringArgument("{<Permission>}"));
                    break;
                }
                completion.add(new StringArgument("{<Name>}"));
                break;
            case "show":
                if (count == 3) {
                    Optional<String> showType = restrict(
                        get(arguments, 2, ArgumentType.STRING).map(BaseArgument::asString), "name", "result",
                        "ingredient");
                    switch (showType.orElse("")) {
                    case "name":
                        completion(completion,
                            RecipesEventsManager
                                .get()
                                .getPermittedRecipes(sender)
                                .stream()
                                .map(CustomRecipe::getName)
                                .filter(value -> value != null)
                                .toArray(String[]::new));
                        break;
                    case "result":
                    case "ingredient":
                        completion(completion,
                            stream(Material.values())
                                .map(material -> material.name().toLowerCase())
                                .filter(name -> !name.startsWith("legacy"))
                                .toArray(String[]::new));
                        completion(completion, OraxenItems.nameArray());
                        break;
                    default:
                        break;
                    }
                    break;
                }
                completion.add(new StringArgument("all"));
                completion.add(new StringArgument("name"));
                completion.add(new StringArgument("result"));
                completion.add(new StringArgument("ingredient"));
                break;
            default:
                break;
            }
        }

        return completion;
    }

}