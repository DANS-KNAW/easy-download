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

/*
 * a user can either be authenticated or not
 * currently you solve this by providing an Option
 * maybe it is more fitting to define various types of users instead
 * and use dynamic dispatch to define behavior for each type of user
 *
 * first classification:
 * sealed abstract class User
 * case class UnauthenticatedUser extends User
 * sealed abstract class AuthenticatedUser extends User // maybe merge this one with the next?
 * case class NormalAuthenticatedUser extends AuthenticatedUser
 * case class ArchivistUser extends AuthenticatedUser
 * case class AdminUser extends AuthenticatedUser
 */
case class User(id: String,
                groups: Seq[String] = Seq.empty,
                isArchivist: Boolean = false,
                isAdmin: Boolean = false
               ) {
  override def toString: String = {
    s"User: id=$id groups=$groups isArchivist=$isArchivist isAdmin=$isAdmin"
  }
}
