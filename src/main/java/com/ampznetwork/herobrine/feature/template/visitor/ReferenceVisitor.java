package com.ampznetwork.herobrine.feature.template.visitor;

import com.ampznetwork.herobrine.antlr.DiscordMessageTemplateParser;
import com.ampznetwork.herobrine.antlr.DiscordMessageTemplateParserBaseVisitor;
import com.ampznetwork.herobrine.feature.template.context.Reference;
import lombok.Value;
import org.antlr.v4.runtime.tree.ParseTree;

@Value
public class ReferenceVisitor extends DiscordMessageTemplateParserBaseVisitor<Reference.Builder> {
    public static final ReferenceVisitor INSTANCE = new ReferenceVisitor();

    public static Reference parse(ParseTree tree) {
        return INSTANCE.visit(tree).build();
    }

    @Override
    public Reference.Builder visitRefChainProperty(DiscordMessageTemplateParser.RefChainPropertyContext ctx) {
        return visit(ctx.reference_chain()).key(new Reference.Part(ctx.WORD().getText(), ctx.QUESTION() != null));
    }

    @Override
    public Reference.Builder visitRefChainFunction(DiscordMessageTemplateParser.RefChainFunctionContext ctx) {
        return visit(ctx.reference_chain()).key(new Reference.Part(ctx.WORD().getText(), ctx.QUESTION() != null))
                .arguments(ctx.arg_list().expression().stream().map(ExpressionVisitor.INSTANCE::visit).toList());
    }

    @Override
    public Reference.Builder visitRefChainBase(DiscordMessageTemplateParser.RefChainBaseContext ctx) {
        return Reference.builder().key(new Reference.Part(ctx.getText(), true));
    }
}
