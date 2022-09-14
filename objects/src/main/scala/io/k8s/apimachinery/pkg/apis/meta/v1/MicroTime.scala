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

package io.k8s.apimachinery.pkg.apis.meta.v1

import dev.hnaderi.k8s.utils._

/** MicroTime is version of Time with microsecond level precision. */
final case class MicroTime(value: String) extends AnyVal
//TODO

object MicroTime {
  implicit def encoder[T](implicit
      builder: Builder[T]
  ): Encoder[MicroTime, T] = new Encoder[MicroTime, T] {
    def apply(r: MicroTime): T = builder.of(r.value)
  }

  implicit def decoder[T: Reader]: Decoder[T, MicroTime] =
    Decoder[T, String].map(MicroTime(_))
}
