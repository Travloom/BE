package com.example.travel_project.controller;

import com.example.travel_project.dto.InviteListResponseDTO;
import com.example.travel_project.entity.InviteList;
import com.example.travel_project.entity.User;
import com.example.travel_project.entity.UserPlanList;
import com.example.travel_project.repository.InviteListRepository;
import com.example.travel_project.repository.UserPlanListRepository;
import com.example.travel_project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/invites")
@RequiredArgsConstructor
public class MyInviteController {
    private final InviteListRepository inviteListRepository;
    private final UserRepository userRepository;
    private final UserPlanListRepository userPlanListRepository;

    @GetMapping("/me")
    public ResponseEntity<List<InviteListResponseDTO>> myInvites(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal
    ) {
        String email = principal.getAttribute("email");
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return ResponseEntity.ok(List.of());

        List<InviteList> invites = inviteListRepository.findByUserId(user.getId());
        List<InviteListResponseDTO> resp = invites.stream().map(invite ->
                new InviteListResponseDTO(
                        invite.getId(),
                        invite.getUser().getName(),
                        invite.getUser().getEmail(),
                        invite.getPlan().getTitle(),
                        invite.getIsAccepted()
                )
        ).collect(Collectors.toList());

        return ResponseEntity.ok(resp);
    }


    @PostMapping("/{inviteId}/accept")
    public ResponseEntity<String> acceptInvite(
            @PathVariable Long inviteId,
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal
    ) {
        InviteList invite = inviteListRepository.findById(inviteId)
                .orElseThrow(() -> new IllegalArgumentException("초대 없음"));
        String email = principal.getAttribute("email");
        if (!invite.getUser().getEmail().equals(email)) {
            return ResponseEntity.status(403).body("권한 없음");
        }
        invite.setIsAccepted(true);
        inviteListRepository.save(invite);

        boolean already = userPlanListRepository
                .findByPlanId(invite.getPlan().getId())
                .stream().anyMatch(upl -> upl.getUser().getEmail().equals(email));
        if (!already) {
            UserPlanList userPlan = new UserPlanList();
            userPlan.setUser(invite.getUser());
            userPlan.setPlan(invite.getPlan());
            userPlanListRepository.save(userPlan); // <- 실제로 저장!
        }

        return ResponseEntity.ok("수락 완료");
    }

}
