package io.github.naimjeg.combatnexus.combat.api.pipeline;

/** Definition registration handle. Closing after freeze is an error, not runtime deactivation. */
@FunctionalInterface
public interface PipelineRegistration extends AutoCloseable {
    @Override void close();
}
