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

package dev.hnaderi.k8s.client

import zio._

object ZIOKubernetesClient {
  def send[O](
      req: HttpRequest[O]
  ): ZIO[HttpClient[Task], Throwable, O] =
    ZIO.service[HttpClient[Task]].flatMap(req.send)

  def make(url: String): ZLayer[ZIOBackend, Nothing, HttpClient[Task]] =
    ZLayer {
      ZIO.service[HttpBackend[Task]].map(HttpClient(url, _))
    }
}
