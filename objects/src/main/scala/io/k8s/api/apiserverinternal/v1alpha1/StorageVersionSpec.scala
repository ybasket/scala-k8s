/*
 * Copyright 2022 Hossein Naderi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.k8s.api.apiserverinternal.v1alpha1

import dev.hnaderi.k8s.utils._

/** StorageVersionSpec is an empty spec. */
final case class StorageVersionSpec()
//TODO

object StorageVersionSpec {
  implicit val encoder: Encoder[StorageVersionSpec] =
    Encoder.emptyObj
  implicit val decoder: Decoder[StorageVersionSpec] =
    Decoder.const(StorageVersionSpec())
}
