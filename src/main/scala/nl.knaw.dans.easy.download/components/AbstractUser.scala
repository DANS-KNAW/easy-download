/**
 * Copyright (C) 2017 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.download.components

// TODO not sure what the `groups` is used for; for now I just kept it in and gave them all an empty Seq
sealed abstract class AbstractUser(id: String, groups: Seq[String])
case object UnauthenticatedUser extends AbstractUser("", Seq.empty)
case class AuthenticatedUser(id: String, groups: Seq[String] = Seq.empty) extends AbstractUser(id, groups)
case class ArchivistUser(id: String, groups: Seq[String] = Seq.empty) extends AbstractUser(id, groups)
case class AdminUser(id: String, groups: Seq[String] = Seq.empty) extends AbstractUser(id, groups)
