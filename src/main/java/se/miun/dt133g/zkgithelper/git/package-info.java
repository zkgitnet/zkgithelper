/**
 * Provides utility classes for interacting with Git repositories using the JGit library.
 * The package includes operations for managing commits, references, and objects in Git repositories.
 * It supports pushing, fetching, and listing changes between repositories,
 * as well as inspecting Git objects and references.
 *
 * Classes:
 * - GitCommands: Offers various methods for low-level Git operations like checking commit ancestry,
 *   retrieving Git object data, and resolving references.
 * - GitHandler: A singleton class responsible for managing Git operations, including pushing, fetching,
 *   and listing repository references and handling both main and temporary repositories.
 */
package se.miun.dt133g.zkgithelper.git;
