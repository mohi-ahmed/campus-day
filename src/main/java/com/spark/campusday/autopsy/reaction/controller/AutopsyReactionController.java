package com.spark.campusday.autopsy.reaction.controller;

import com.spark.campusday.autopsy.reaction.dto.ReactionRequest;
import com.spark.campusday.autopsy.reaction.dto.ReactionResponse;
import com.spark.campusday.autopsy.reaction.dto.RepostResponse;
import com.spark.campusday.autopsy.reaction.dto.SessionRequest;
import com.spark.campusday.autopsy.reaction.service.AutopsyReactionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/autopsy/posts/{postId}")
@RequiredArgsConstructor
public class AutopsyReactionController {
    private final AutopsyReactionService reactions;

    @PostMapping("/reactions")
    public ReactionResponse react(
            @PathVariable @NotBlank String postId,
            @Valid @RequestBody ReactionRequest request) {
        return reactions.react(postId, request);
    }

    @PostMapping("/reposts")
    public RepostResponse repost(
            @PathVariable @NotBlank String postId,
            @Valid @RequestBody SessionRequest request) {
        return reactions.toggleRepost(postId, request);
    }
}
