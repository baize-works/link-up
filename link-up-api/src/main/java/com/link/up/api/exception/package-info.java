/**
 * Shared public error contract for API consumers, framework code, and connectors.
 *
 * <p>Error codes and the base exception live beside the public API rather than in a generic
 * utility module, so extension authors only need the API artifact to report structured failures.
 */
package com.link.up.api.exception;
