package com.burcuduzen.dayflow.planning;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/planning/blocks")
public class PlannedBlockController {
    public record ApprovalRequest(@NotEmpty List<@Valid ApprovalBlock> blocks) {}
    public record ApprovalBlock(@NotNull @Positive Long taskId, @NotNull OffsetDateTime startAt,
                                @NotNull OffsetDateTime endAt) {}
    public record BlockResponse(Long id, Long taskId, OffsetDateTime startAt,
                                OffsetDateTime endAt, OffsetDateTime createdAt) {
        static BlockResponse from(PlannedBlock block) {
            return new BlockResponse(block.getId(), block.getTaskId(), block.getStartAt(),
                block.getEndAt(), block.getCreatedAt());
        }
    }

    private final PlannedBlockService service;
    public PlannedBlockController(PlannedBlockService service) { this.service = service; }

    @GetMapping
    public List<BlockResponse> list(@RequestParam(required = false) OffsetDateTime from,
                                    @RequestParam(required = false) OffsetDateTime to) {
        return service.list(from, to).stream().map(BlockResponse::from).toList();
    }

    @PostMapping("/approve")
    public List<BlockResponse> approve(@Valid @RequestBody ApprovalRequest request) {
        return service.approve(request.blocks().stream().map(block -> new PlannedBlockService.Approval(
            block.taskId(), block.startAt(), block.endAt())).toList()).stream().map(BlockResponse::from).toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
